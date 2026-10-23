package fun.hotp;

import javacard.framework.APDU;
import javacard.framework.Applet;
import javacard.framework.ISO7816;
import javacard.framework.ISOException;
import javacard.framework.JCSystem;
import javacard.framework.SystemException;
import javacard.framework.Util;
import javacard.security.CryptoException;
import javacard.security.HMACKey;
import javacard.security.KeyBuilder;
import javacard.security.MessageDigest;
import javacard.security.Signature;
import javacardx.crypto.Cipher;
import uicc.toolkit.ToolkitInterface;
import uicc.toolkit.ToolkitRegistry;
import uicc.toolkit.ToolkitRegistrySystem;
import uicc.system.UICCPlatform;
import uicc.toolkit.EnvelopeHandler;
import uicc.toolkit.EnvelopeHandlerSystem;
import uicc.toolkit.ProactiveHandler;
import uicc.toolkit.ProactiveHandlerSystem;
import uicc.toolkit.ProactiveResponseHandler;
import uicc.toolkit.ProactiveResponseHandlerSystem;
import uicc.toolkit.ToolkitConstants;
import uicc.toolkit.ToolkitException;

public class
UICC_HOTP extends Applet implements ToolkitInterface, ToolkitConstants
{
    private static final short SW_TOOLKIT_EXCEPTION = 0x6600;
    private static final short SW_ARRAY_BOUNDARY_EXCEPTION = 0x6F11;
    private static final short SW_NULL_PTR_EXCEPTION = 0x6F20;
    private static final short SW_CRYPTO_EXCEPTION = 0x6F30;
    private static final short SW_SYSTEM_EXCEPTION = 0x6F40;
    private static final short SW_UNKNOWN_EXCEPTION = 0x6F50;

    private static final short STR_ALPHA_ID_LEN = 11;
    private static final short STR_ALPHA_ID_OFF = 0;

    private static final byte  ITEM_ID_HOTP   = 0x01;
    private static final short STR_MENU_1_LEN = 21;
    private static final short STR_MENU_1_OFF = (short)(STR_ALPHA_ID_OFF + STR_ALPHA_ID_LEN);

    private static final byte  ITEM_ID_HELP   = 0x02;
    private static final short STR_MENU_2_LEN = 15;
    private static final short STR_MENU_2_OFF = (short)(STR_MENU_1_OFF + STR_MENU_1_LEN);

    private static final short STR_ALL_RIGHT_LEN = 14;
    private static final short STR_ALL_RIGHT_OFF = (short)(STR_MENU_2_OFF + STR_MENU_2_LEN);


    private static final byte[] strings = {
        // Alpha ID: SIM HOTP
        (byte)0x81,(byte)0x08,(byte)0x08,(byte)0x53,(byte)0x49,(byte)0x4D,(byte)0x20,(byte)0x48,
        (byte)0x4F,(byte)0x54,(byte)0x50,
        // Menu 1: Сгенерировать HOTP
        (byte)0x81,(byte)0x12,(byte)0x08,(byte)0xA1,(byte)0xB3,(byte)0xB5,(byte)0xBD,(byte)0xB5,
        (byte)0xC0,(byte)0xB8,(byte)0xC0,(byte)0xBE,(byte)0xB2,(byte)0xB0,(byte)0xC2,(byte)0xCC,
        (byte)0x20,(byte)0x48,(byte)0x4F,(byte)0x54,(byte)0x50,
        // Menu 2: Нужна помощь
        (byte)0x81,(byte)0x0C,(byte)0x08,(byte)0x9D,(byte)0xC3,(byte)0xB6,(byte)0xBD,(byte)0xB0,
        (byte)0x20,(byte)0xBF,(byte)0xBE,(byte)0xBC,(byte)0xBE,(byte)0xC9,(byte)0xCC,
        // For testing purposes: 'Все Ок!'
        (byte)0x04,(byte)0x12,(byte)0x04,(byte)0x41,(byte)0x04,(byte)0x35,(byte)0x00,(byte)0x20,
        (byte)0x04,(byte)0x1E,(byte)0x04,(byte)0x3A,(byte)0x00,(byte)0x21,
    };

    private static final short _00 = 0x00;
    private static final short _01 = 0x01;
    private static final short _04 = 0x04;
    private static final short _08 = 0x08;
    private static final short _20 = 0x14;
    private static final short _32 = 0x20;
    private static final short _64 = 0x40;
    
    private static final byte QLFR_DATE_TIME_INFO = 0x03;
    private static final short HMAC_KEY_LENGTH_512_BITS = _64;

    private ToolkitRegistry reg;
    private byte iid_sim_hotp;

    public HMACKey hmac_key;
    public Signature hmac_sign;

    public UICC_HOTP(byte[] install_params, short offset, byte length)
    {
        length = install_params[offset++]; // instance AID length
		offset += length;
		length = install_params[offset++]; // Control info length
		offset += length;
		length = install_params[offset++]; // Grab the length of HMAC key
        
        try {
            hmac_key = (HMACKey)KeyBuilder.buildKey(KeyBuilder.ALG_TYPE_HMAC, JCSystem.MEMORY_TYPE_PERSISTENT, (short)(length * 8), false);

            // Initialize the HMAC key with the given bytes
            hmac_key.setKey(install_params, offset, length);
            hmac_sign = Signature.getInstance(MessageDigest.ALG_SHA_256, Signature.SIG_CIPHER_HMAC, Cipher.PAD_NULL, true);
        } catch (CryptoException e) {
            ISOException.throwIt((short)(SW_CRYPTO_EXCEPTION + 1));
        }
    }

    public static void
    install(byte[] install_params, short offset, byte length)
    {
        UICC_HOTP applet = new UICC_HOTP(install_params, offset, length);
        applet.register();

        applet.reg = ToolkitRegistrySystem.getEntry();
        applet.iid_sim_hotp = applet.reg.initMenuEntry(strings, STR_ALPHA_ID_OFF, STR_ALPHA_ID_LEN, PRO_CMD_SELECT_ITEM, false, (byte)_00, _00);
    }

    public void
    processToolkit(short event)
    {
        EnvelopeHandler env_hdlr;
        byte item_id;

        try {
            // hmac_key.setKey(secret_key, _00, _08);
            switch (event) {
                case EVENT_MENU_SELECTION: {
                    env_hdlr = EnvelopeHandlerSystem.getTheHandler();
                    item_id  = env_hdlr.getItemIdentifier();
    
                    if (item_id == iid_sim_hotp) {
                        hotp_submenu();
                    }
                } break;
                default: return;
            }
        }
        catch (ArrayIndexOutOfBoundsException exc) {error_logger(SW_ARRAY_BOUNDARY_EXCEPTION);}
        catch (NullPointerException exc) {error_logger(SW_NULL_PTR_EXCEPTION);}
        catch (ISOException exc)         {error_logger(exc.getReason());}
        catch (ToolkitException exc)     {error_logger((short)(SW_TOOLKIT_EXCEPTION | exc.getReason()));}
        catch (CryptoException exc)      {error_logger((short)(SW_CRYPTO_EXCEPTION  | exc.getReason()));}
        catch (SystemException exc)      {error_logger((short)(SW_SYSTEM_EXCEPTION  | exc.getReason()));}
        catch (Exception exc)            {error_logger(SW_UNKNOWN_EXCEPTION);}
        
    }

    protected void
    hotp_submenu()
    {
        byte tag = (byte)(TAG_ITEM | TAG_SET_CR);
        ProactiveHandler pro_hdlr = ProactiveHandlerSystem.getTheHandler();
        ProactiveResponseHandler pro_resp_hdlr = ProactiveResponseHandlerSystem.getTheHandler();

        pro_hdlr.init(PRO_CMD_SELECT_ITEM, (byte)_00, DEV_ID_TERMINAL);
        pro_hdlr.appendTLV(TAG_ALPHA_IDENTIFIER, strings, STR_ALPHA_ID_OFF, STR_ALPHA_ID_LEN);

        pro_hdlr.appendTLV(tag, ITEM_ID_HOTP, strings, STR_MENU_1_OFF, STR_MENU_1_LEN);
        pro_hdlr.appendTLV(tag, ITEM_ID_HELP, strings, STR_MENU_2_OFF, STR_MENU_2_LEN);

        if (RES_CMD_PERF != pro_hdlr.send()) {
            return;
        }

        switch (pro_resp_hdlr.getItemIdentifier()) {
            case ITEM_ID_HOTP:
                option_totp(pro_hdlr, pro_resp_hdlr);
            break;
            case ITEM_ID_HELP:
                option_help(pro_hdlr, pro_resp_hdlr);
            break;
        }

    }

    private void
    option_totp(ProactiveHandler pro_hdlr, ProactiveResponseHandler pro_resp_hdlr)
    {
        short len;
        byte[] global_buff;

        pro_hdlr.init(PRO_CMD_PROVIDE_LOCAL_INFORMATION, QLFR_DATE_TIME_INFO, DEV_ID_TERMINAL);
        if (pro_hdlr.send() != RES_CMD_PERF) {
            ISOException.throwIt((short)(SW_TOOLKIT_EXCEPTION | 0x13));
        }

        if (pro_resp_hdlr.findTLV(TAG_DATE_TIME_AND_TIME_ZONE, (byte)_01) == TLV_NOT_FOUND) {
            ISOException.throwIt((short)(SW_TOOLKIT_EXCEPTION | 0x14));
        }

        global_buff = UICCPlatform.getTheVolatileByteArray();
        len         = pro_resp_hdlr.getValueLength();
        pro_resp_hdlr.copyValue(_00, global_buff, _00, len);

        hmac_sign.init(hmac_key, Signature.MODE_SIGN);
        // hmac_sign.sign(global_buff, _00, _08, global_buff, _00);
        pro_hdlr.initDisplayText((byte)0x81, (byte)_08, strings, STR_ALL_RIGHT_OFF, STR_ALL_RIGHT_LEN);
        pro_hdlr.send();
    }

    private void
    option_help(ProactiveHandler pro_hdlr, ProactiveResponseHandler pro_resp_hdlr)
    {
        pro_hdlr.initDisplayText((byte)0x81, (byte)_08, strings, STR_ALL_RIGHT_OFF, STR_ALL_RIGHT_LEN);
        pro_hdlr.send();
    }

    private void
    error_logger(short sw)
    {
        ProactiveHandler pro_hdlr = ProactiveHandlerSystem.getTheHandler();
        byte[] global_buff = UICCPlatform.getTheVolatileByteArray();

        to_ascii(global_buff, sw);
        
        pro_hdlr.initDisplayText((byte)0x81, (byte)_04, global_buff, _00, _08);
        pro_hdlr.send();
    }

    private void
    to_ascii(byte[] global_buff, short sw)
    {
        short offset = 0;

        // SW:
        global_buff[offset++] = (byte)0x53;
        global_buff[offset++] = (byte)0x57;
        global_buff[offset++] = (byte)0x3A;
        global_buff[offset++] = (byte)0x20;

        // four bytes for status word
        global_buff[offset++] = hex_nibble((byte)((sw & (short)0xF000) >> 12));
        global_buff[offset++] = hex_nibble((byte)((sw & 0x0F00) >> 8));
        global_buff[offset++] = hex_nibble((byte)((sw & 0x00F0) >> 4));
        global_buff[offset++] = hex_nibble((byte)((sw & 0x000F) >> 0));
    }

    private byte
    hex_nibble(byte nibble)
    {
        byte res = 0x30;
        if (nibble <= 9) {
            res = (byte)(0x30 + nibble);
        }
        else if (nibble <= 15) {
            res = (byte)(0x37 + nibble);
        }
        
        return res;
    }

    public void
    process(APDU apdu) { /* Remains unused in the STK context */ }
}