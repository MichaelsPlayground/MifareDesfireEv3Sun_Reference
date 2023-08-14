package de.androidcrypto.desfireev3sun;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import android.os.Bundle;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;

public class MainActivity extends AppCompatActivity {

    public static final byte[] APPLICATION_KEY_MASTER_AES_DEFAULT = Utils.hexStringToByteArray("00000000000000000000000000000000"); // default AES key with 16 nulls
    public static byte[] APPLICATION_KEY_MASTER_AES = Utils.hexStringToByteArray("A08899AABBCCDD223344556677889911");
    public static final byte APPLICATION_KEY_MASTER_NUMBER = (byte) 0x00;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        //bottomNav.setOnNavigationItemSelectedListener(navListener);
        bottomNav.setOnItemSelectedListener(navListener);

        // as soon as the application opens the first
        // fragment should be shown to the user
        // in this case it is algorithm fragment
        getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, new HomeFragment()).commit();
    }

    //private final BottomNavigationView.OnNavigationItemSelectedListener navListener = item -> {
    private final NavigationBarView.OnItemSelectedListener navListener = item -> {
        // By using switch we can easily get
        // the selected fragment
        // by using there id.
        Fragment selectedFragment = null;
        int itemId = item.getItemId();
        if (itemId == R.id.home) {
            selectedFragment = new HomeFragment();
        } else if (itemId == R.id.read_ndef_content) {
            selectedFragment = new ReadNdefContentFragment();
        } else if (itemId == R.id.prepare_sdm) {
            selectedFragment = new PrepareSdmFragment();
        } else if (itemId == R.id.activate_sdm) {
            selectedFragment = new ActivateSdmFragment();
        } else if (itemId == R.id.format_picc) {
            selectedFragment = new FormatPiccFragment();
      }
        // It will help to replace the
        // one fragment to other.
        if (selectedFragment != null) {
            getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, selectedFragment).commit();
        }
        return true;
    };
}