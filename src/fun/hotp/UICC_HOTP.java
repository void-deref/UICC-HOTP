package fun.hotp;

import javacard.framework.APDU;
import javacard.framework.Applet;
import javacard.framework.ISOException;
import javacard.framework.SystemException;
import javacard.security.CryptoException;
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

    private static final short STR_PROVIDE_LOCAL_INFO_ERR_LEN = 110;
    private static final short STR_PROVIDE_LOCAL_INFO_ERR_OFF = (short)(STR_ALL_RIGHT_OFF + STR_ALL_RIGHT_LEN);
    
    private static final short STR_DATE_TIME_INFO_ERR_LEN = 108;
    private static final short STR_DATE_TIME_INFO_ERR_OFF = (short)(STR_PROVIDE_LOCAL_INFO_ERR_OFF + STR_PROVIDE_LOCAL_INFO_ERR_LEN);

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
        // ERROR MESSAGE: Телефон не ответил на команду PROVIDE LOCAL INFORMATION
        (byte)0x04,(byte)0x22,(byte)0x04,(byte)0x35,(byte)0x04,(byte)0x3B,(byte)0x04,(byte)0x35,
        (byte)0x04,(byte)0x44,(byte)0x04,(byte)0x3E,(byte)0x04,(byte)0x3D,(byte)0x00,(byte)0x20,
        (byte)0x04,(byte)0x3D,(byte)0x04,(byte)0x35,(byte)0x00,(byte)0x20,(byte)0x04,(byte)0x3E,
        (byte)0x04,(byte)0x42,(byte)0x04,(byte)0x32,(byte)0x04,(byte)0x35,(byte)0x04,(byte)0x42,
        (byte)0x04,(byte)0x38,(byte)0x04,(byte)0x3B,(byte)0x00,(byte)0x20,(byte)0x04,(byte)0x3D,
        (byte)0x04,(byte)0x30,(byte)0x00,(byte)0x20,(byte)0x04,(byte)0x3A,(byte)0x04,(byte)0x3E,
        (byte)0x04,(byte)0x3C,(byte)0x04,(byte)0x30,(byte)0x04,(byte)0x3D,(byte)0x04,(byte)0x34,
        (byte)0x04,(byte)0x43,(byte)0x00,(byte)0x20,(byte)0x00,(byte)0x50,(byte)0x00,(byte)0x52,
        (byte)0x00,(byte)0x4F,(byte)0x00,(byte)0x56,(byte)0x00,(byte)0x49,(byte)0x00,(byte)0x44,
        (byte)0x00,(byte)0x45,(byte)0x00,(byte)0x20,(byte)0x00,(byte)0x4C,(byte)0x00,(byte)0x4F,
        (byte)0x00,(byte)0x43,(byte)0x00,(byte)0x41,(byte)0x00,(byte)0x4C,(byte)0x00,(byte)0x20,
        (byte)0x00,(byte)0x49,(byte)0x00,(byte)0x4E,(byte)0x00,(byte)0x46,(byte)0x00,(byte)0x4F,
        (byte)0x00,(byte)0x52,(byte)0x00,(byte)0x4D,(byte)0x00,(byte)0x41,(byte)0x00,(byte)0x54,
        (byte)0x00,(byte)0x49,(byte)0x00,(byte)0x4F,(byte)0x00,(byte)0x4E,

        // ERROR MESSAGE: Телефон не передал информацию о текущей дате и времени
        (byte)0x04,(byte)0x22,(byte)0x04,(byte)0x35,(byte)0x04,(byte)0x3B,(byte)0x04,(byte)0x35,
        (byte)0x04,(byte)0x44,(byte)0x04,(byte)0x3E,(byte)0x04,(byte)0x3D,(byte)0x00,(byte)0x20,
        (byte)0x04,(byte)0x3D,(byte)0x04,(byte)0x35,(byte)0x00,(byte)0x20,(byte)0x04,(byte)0x3F,
        (byte)0x04,(byte)0x35,(byte)0x04,(byte)0x40,(byte)0x04,(byte)0x35,(byte)0x04,(byte)0x34,
        (byte)0x04,(byte)0x30,(byte)0x04,(byte)0x3B,(byte)0x00,(byte)0x20,(byte)0x04,(byte)0x38,
        (byte)0x04,(byte)0x3D,(byte)0x04,(byte)0x44,(byte)0x04,(byte)0x3E,(byte)0x04,(byte)0x40,
        (byte)0x04,(byte)0x3C,(byte)0x04,(byte)0x30,(byte)0x04,(byte)0x46,(byte)0x04,(byte)0x38,
        (byte)0x04,(byte)0x4E,(byte)0x00,(byte)0x20,(byte)0x04,(byte)0x3E,(byte)0x00,(byte)0x20,
        (byte)0x04,(byte)0x42,(byte)0x04,(byte)0x35,(byte)0x04,(byte)0x3A,(byte)0x04,(byte)0x43,
        (byte)0x04,(byte)0x49,(byte)0x04,(byte)0x35,(byte)0x04,(byte)0x39,(byte)0x00,(byte)0x20,
        (byte)0x04,(byte)0x34,(byte)0x04,(byte)0x30,(byte)0x04,(byte)0x42,(byte)0x04,(byte)0x35,
        (byte)0x00,(byte)0x20,(byte)0x04,(byte)0x38,(byte)0x00,(byte)0x20,(byte)0x04,(byte)0x32,
        (byte)0x04,(byte)0x40,(byte)0x04,(byte)0x35,(byte)0x04,(byte)0x3C,(byte)0x04,(byte)0x35,
        (byte)0x04,(byte)0x3D,(byte)0x04,(byte)0x38,


    };

    private static final short _00 = 0x00;
    private static final short _01 = 0x01;
    private static final short _08 = 0x08;
    private static final byte QLFR_DATE_TIME_INFO = 0x03;

    private ToolkitRegistry reg;
    private byte iid_sim_hotp;

    public UICC_HOTP()
    {
        
    }

    public static void
    install(byte[] install_params, short offset, byte length)
    {
        UICC_HOTP applet = new UICC_HOTP();
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
        } catch (ToolkitException e) {
            ProactiveHandler pro_hdlr = ProactiveHandlerSystem.getTheHandler();
            pro_hdlr.initDisplayText((byte)0x81, (byte)_08, strings, STR_PROVIDE_LOCAL_INFO_ERR_OFF, STR_PROVIDE_LOCAL_INFO_ERR_LEN);
            pro_hdlr.send();
        }
        
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
            pro_hdlr.initDisplayText((byte)0x81, (byte)_08, strings, STR_PROVIDE_LOCAL_INFO_ERR_OFF, STR_PROVIDE_LOCAL_INFO_ERR_LEN);
            pro_hdlr.send();
            return;
        }

        if (pro_resp_hdlr.findTLV(TAG_DATE_TIME_AND_TIME_ZONE, (byte)_01) == TLV_NOT_FOUND) {
            pro_hdlr.initDisplayText((byte)0x81, (byte)_08, strings, STR_DATE_TIME_INFO_ERR_OFF, STR_DATE_TIME_INFO_ERR_LEN);
            pro_hdlr.send();
            return;
        }

        global_buff = UICCPlatform.getTheVolatileByteArray();
        // len = pro_resp_hdlr.getValueLength();
        // pro_resp_hdlr.copyValue(_00, global_buff, _00, len);

        pro_hdlr.initDisplayText((byte)0x81, (byte)_08, strings, STR_ALL_RIGHT_OFF, STR_ALL_RIGHT_LEN);
        pro_hdlr.send();
    }

    private void
    option_help(ProactiveHandler pro_hdlr, ProactiveResponseHandler pro_resp_hdlr)
    {
        pro_hdlr.initDisplayText((byte)0x81, (byte)_08, strings, STR_ALL_RIGHT_OFF, STR_ALL_RIGHT_LEN);
        pro_hdlr.send();
    }

    public void
    process(APDU apdu) { /* Remains unused in the STK context */ }
}