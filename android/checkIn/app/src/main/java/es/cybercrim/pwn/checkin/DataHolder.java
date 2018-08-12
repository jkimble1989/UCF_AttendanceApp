package es.cybercrim.pwn.checkin;

public class DataHolder {
    private static String data;
    private static String data2;
    private static String data3;

    public static String getData() {
        return data;
    }

    public static String getData2() {
        return data2;
    }

    public static String getData3() {
        return data3;
    }

    public static void setData(String data, String data2, String data3) {
        DataHolder.data = data;
        DataHolder.data2 = data2;
        DataHolder.data3 = data3;
    }
}