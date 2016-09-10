package com.example.android.bluetoothchat;

/**
 * Created by Chun-Wei Tseng on 2016/2/17.
 */
public class OBD {

//    ERR 43 FF FF FF .....,  //03
//    FSS 41 03 FF FF,//0103
//    ELV 41 04 FF,//0104
//    EC 41 05 FF, //0105
//    AT 41 0F FF, //010F
//    RPM 41 0C FF FF, //010C
//    SPD 41 0D FF , //010D

    public static final String VERSION_CMD = "ATI";
    public static final String ECHOOFF_CMD = "ATE0";
    public static final String INIT_CMD= "ATSP0";

    public static final String P_ERR_CMD = "07";
    public static final String ERR_CMD = "03";
    public static final String FSS_CMD = "0103";
    public static final String ELV_CMD  = "0104";
    public static final String EC_CMD  = "0105";
    public static final String AT_CMD  = "010F";
//    public static final String FLI_CMD  = "012F";

    public static final String RPM_CMD  = "010C";
    public static final String SPD_CMD  = "010D";
    public static final String UNDEFINED = "UND";

    public static String getResponseString(String code,String PID){

            if(code.equals("47")){
               //MODE 7
                // Pending Error codes
                return "PERR";
            } else if (code.equals("43")) {
                //MODE 3
                // This mode shows the stored diagnostic trouble codes
                return "ERR";
            }else if(code.equals("41")){
                //MODE 1
                //This mode returns the common values for some sensors
                if (PID.equals("03")){
                    //Fuel system status
                    return "FSS";
                } else if (PID.equals("04")){
                    //Engine load calculated in %
                    return "ELV";
                }else if (PID.equals("05")){
                    //Temperature of the engine coolant in Â°C
                    return "EC";
                }else if (PID.equals("0F")){
                    //Intake air temperature in Â°C
                    return "AT";
                }else if (PID.equals("0C")){
                    //Engine speed in rpm
                    return "RPM";
                }else if (PID.equals("0D")){
                    //Vehicle speed in kph
                    return "SPD";
                }
            }

        return UNDEFINED;
    }


}

