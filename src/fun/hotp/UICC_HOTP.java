package fun.hotp;

import javacard.framework.APDU;
import javacard.framework.Applet;
import uicc.toolkit.ToolkitInterface;
import uicc.toolkit.ToolkitRegistry;
import uicc.toolkit.ToolkitRegistrySystem;
import uicc.toolkit.ToolkitConstants;

public class
UICC_HOTP extends Applet implements ToolkitInterface, ToolkitConstants
{
    private static final short STR_ALPHA_ID_LEN = 11;
    private static final short STR_ALPHA_ID_OFF = 0;

    private static final short STR_MENU_1_LEN = 21;
    private static final short STR_MENU_1_OFF = (short)(STR_ALPHA_ID_OFF + STR_ALPHA_ID_LEN);

    private static final short STR_MENU_2_LEN = 15;
    private static final short STR_MENU_2_OFF = (short)(STR_MENU_1_OFF + STR_MENU_1_LEN);

    private static final byte[] strings = {
        // Alpha ID: SIM HOTP
        (byte)0x81,(byte)0x08,(byte)0x08,(byte)0x53,(byte)0x49,(byte)0x4D,(byte)0x20,(byte)0x48,(byte)0x4F,(byte)0x54,(byte)0x50,
        // Menu 1: Сгенерировать HOTP
        (byte)0x81,(byte)0x12,(byte)0x08,(byte)0xA1,(byte)0xB3,(byte)0xB5,(byte)0xBD,(byte)0xB5,(byte)0xC0,(byte)0xB8,(byte)0xC0,(byte)0xBE,(byte)0xB2,(byte)0xB0,(byte)0xC2,(byte)0xCC,(byte)0x20,(byte)0x48,(byte)0x4F,(byte)0x54,(byte)0x50,
        // Menu 2: Нужна помощь
        (byte)0x81,(byte)0x0C,(byte)0x08,(byte)0x9D,(byte)0xC3,(byte)0xB6,(byte)0xBD,(byte)0xB0,(byte)0x20,(byte)0xBF,(byte)0xBE,(byte)0xBC,(byte)0xBE,(byte)0xC9,(byte)0xCC,

    };

    private static final short _00 = 0x00;

    ToolkitRegistry reg;
    private byte sim_hotp_menu_id;

    public UICC_HOTP()
    {
        
    }

    public static void
    install(byte[] install_params, short offset, byte length)
    {
        UICC_HOTP applet = new UICC_HOTP();
        applet.register();
        applet.reg = ToolkitRegistrySystem.getEntry();
        applet.sim_hotp_menu_id = applet.reg.initMenuEntry(strings, STR_ALPHA_ID_OFF, STR_ALPHA_ID_LEN, PRO_CMD_SELECT_ITEM, false, (byte)_00, _00);
    }

    public void
    processToolkit(short event)
    {

    }

    public void
    process(APDU apdu)
    {

    }
}