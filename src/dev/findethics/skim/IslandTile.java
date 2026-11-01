package dev.findethics.skim;
import android.service.quicksettings.*;
public class IslandTile extends TileService {
    @Override public void onStartListening() { getQsTile().setState(getSharedPreferences("island", MODE_PRIVATE).getBoolean("enabled", true) ? Tile.STATE_ACTIVE : Tile.STATE_INACTIVE); getQsTile().updateTile(); }
    @Override public void onClick() { android.content.SharedPreferences p = getSharedPreferences("island", MODE_PRIVATE); p.edit().putBoolean("enabled", !p.getBoolean("enabled", true)).apply(); onStartListening(); }
}
