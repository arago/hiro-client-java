package co.arago.hiro.client.model;

import co.arago.hiro.client.util.JsonTools;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public class HiroItemData {

    public List<HiroResponse> items;

    public static HiroItemData fromInputStream(InputStream inputStream) throws IOException {
        return JsonTools.DEFAULT.toObject(inputStream, HiroItemData.class);
    }

}
