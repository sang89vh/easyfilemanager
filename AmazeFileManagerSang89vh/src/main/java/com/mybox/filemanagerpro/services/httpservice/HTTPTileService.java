package com.mybox.filemanagerpro.services.httpservice;

import android.annotation.TargetApi;
import android.content.Intent;
import android.os.Build;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;
import android.widget.Toast;

import com.mybox.filemanagerpro.R;

/**
 * Created by vishal on 1/1/17.
 */

@TargetApi(Build.VERSION_CODES.N)
public class HTTPTileService extends TileService {

    // callbacks are not guaranteed to be called serially, so initialize this before use
    private Tile mTile;

    @Override
    public void onTileAdded() {
        super.onTileAdded();
        mTile = getQsTile();
        mTile.setState(Tile.STATE_INACTIVE);
        mTile.updateTile();
    }

    @Override
    public void onClick() {
        super.onClick();

        mTile = getQsTile();

        if (!HTTPService.isRunning()) {
            if (HTTPService.isConnectedToWifi(getApplicationContext())) {
                startServer();
                mTile.setState(Tile.STATE_ACTIVE);
                mTile.updateTile();
            }
            else {
                mTile.setState(Tile.STATE_INACTIVE);
                mTile.updateTile();
                Toast.makeText(getApplicationContext(), getString(R.string.ftp_no_wifi), Toast.LENGTH_LONG).show();
            }
        } else {
            stopServer();
            mTile.setState(Tile.STATE_INACTIVE);
            mTile.updateTile();
        }
    }

    /**
     * Sends a broadcast to start HTTP server
     */
    private void startServer() {
        getApplicationContext().sendBroadcast(new Intent(HTTPService.ACTION_START_HTTPSERVER));
    }

    /**
     * Sends a broadcast to stop HTTP server
     */
    private void stopServer() {
        getApplicationContext().sendBroadcast(new Intent(HTTPService.ACTION_STOP_HTTPSERVER));
    }
}
