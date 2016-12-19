import com.benli.iqiyi.pojo.OnePiece;
import com.google.gson.Gson;

import java.io.*;
import java.util.List;

/**
 * Created by shibenli on 2016/12/19.
 */
public class TestOnepiece {

    public static void main(String [] args) throws IOException {
        getOnePieceInfo("one_piece/urls/jsonp5", new File("one_piece/json/jsonp5.json"));
        getOnePieceInfo("one_piece/urls/jsonp6", new File("one_piece/json/jsonp6.json"));
    }

    public static void getOnePieceInfo(String fileName, File jsonFile)  throws IOException {
        StringBuffer jsonBuffer = new StringBuffer();
        int size = 0;
        FileInputStream fis = new FileInputStream(jsonFile);
        BufferedInputStream bis = new BufferedInputStream(fis);
        BufferedReader reader = new BufferedReader (new InputStreamReader(bis));
        while (reader.ready()) {
            jsonBuffer.append(reader.readLine());
        }

        getOnePieceInfo(fileName, jsonBuffer.toString());
    }
    public static void getOnePieceInfo(String fileName, String jsonStr)  throws IOException {
        OnePiece onePiece = new Gson().fromJson(jsonStr, OnePiece.class);
        List<OnePiece.DataBean.VlistBean> vlist = onePiece.getData().getVlist();

        StringBuilder contents = new StringBuilder();
        for (OnePiece.DataBean.VlistBean liVlistBean: vlist) {
            contents.append(liVlistBean.getShortTitle()).append(">").append(liVlistBean.getVurl()).append("\n");
        }

        FileOutputStream fos = new FileOutputStream(fileName + ".csv");
        fos.write(contents.toString().getBytes());
        fos.close();
    }
}
