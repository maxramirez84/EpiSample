package org.path.episample.android.activities;

import android.os.Bundle;
import android.app.Activity;

import org.path.episample.android.R;

import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.views.MapView;

public class MapActivity extends Activity {
    private String CKey = "FQ5hUk0oTuXUVCBS3kkTrFJnPTurQ09T4e3zrWlw";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        org.osmdroid.tileprovider.constants.OpenStreetMapTileProviderConstants.setUserAgentValue(CKey);

        MapView map = (MapView) findViewById(R.id.map);
        map.setTileSource(TileSourceFactory.MAPNIK);
    }

}
