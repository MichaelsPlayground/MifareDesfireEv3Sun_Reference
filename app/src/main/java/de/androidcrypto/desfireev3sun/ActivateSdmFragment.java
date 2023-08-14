package de.androidcrypto.desfireev3sun;

import static android.content.Context.VIBRATOR_SERVICE;
import static de.androidcrypto.desfireev3sun.MainActivity.APPLICATION_KEY_MASTER_AES_DEFAULT;
import static de.androidcrypto.desfireev3sun.MainActivity.APPLICATION_KEY_MASTER_NUMBER;
import static de.androidcrypto.desfireev3sun.Utils.printData;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.IsoDep;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.textfield.TextInputLayout;

import java.io.IOException;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link ActivateSdmFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class ActivateSdmFragment extends Fragment implements NfcAdapter.ReaderCallback {
    private static final String TAG = ActivateSdmFragment.class.getName();

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    private LinearLayout llUrl;
    private com.google.android.material.textfield.TextInputEditText output, etCommunicationSettings, etAccessRights;
    private com.google.android.material.textfield.TextInputEditText etSdmReadCounterLimit, etSdmAccessRights, etBaseUrl, etTemplateUrl;
    private com.google.android.material.textfield.TextInputLayout outputLayout, etSdmReadCounterLimitLayout, etSdmAccessRightsLayout;
    private RadioGroup rgStatus;
    private RadioButton rbActivateSdmGetStatus, rbActivateSdmOn, rbActivateSdmOff;
    private CheckBox cbSdmEnabled, cbAsciiEncoding, cbUidMirror, cbReadCounterMirror, cbUidReadCounterEncrypted, cbReadCounterLimit, cbEncryptedFileDataMirror;

    /**
     * general constants
     */

    private byte[] NDEF_APPLICATION_ID = new byte[]{(byte) 0x01, (byte) 0x00, (byte) 0x00};
    private byte NDEF_FILE_ID = (byte) 0x02;
    private final int COLOR_GREEN = Color.rgb(0, 255, 0);
    private final int COLOR_RED = Color.rgb(255, 0, 0);


    // variables for NFC handling
    private NfcAdapter mNfcAdapter;
    private IsoDep isoDep;
    private byte[] tagIdByte;
    private DesfireEv3Light desfireEv3;
    private FileSettings fileSettings;
    private boolean isEncryptedPiccData = false;
    private boolean isDesfireEv3 = false;


    public ActivateSdmFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment SendFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static ActivateSdmFragment newInstance(String param1, String param2) {
        ActivateSdmFragment fragment = new ActivateSdmFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_activate_sdm, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {

        output = getView().findViewById(R.id.etActivateSdmOutput);
        outputLayout = getView().findViewById(R.id.etActivateSdmOutputLayout);

        llUrl = getView().findViewById(R.id.llUrlSettings);
        rgStatus = getView().findViewById(R.id.rgActivateSdmStatus);
        rbActivateSdmGetStatus = getView().findViewById(R.id.rbActivateSdmShowStatus);
        rbActivateSdmOn = getView().findViewById(R.id.rbActivateSdmOn);
        rbActivateSdmOff = getView().findViewById(R.id.rbActivateSdmOff);

        etCommunicationSettings = getView().findViewById(R.id.etActivateSdmCommunicationSettings);
        etAccessRights = getView().findViewById(R.id.etActivateSdmAccessRights);
        cbSdmEnabled = getView().findViewById(R.id.cbActivateSdmAccessSdmEnabled);
        cbAsciiEncoding = getView().findViewById(R.id.cbActivateSdmAsciiEncoding);
        cbUidMirror = getView().findViewById(R.id.cbActivateSdmUidMirror);
        cbReadCounterMirror = getView().findViewById(R.id.cbActivateSdmReadCounterMirror);
        cbUidReadCounterEncrypted = getView().findViewById(R.id.cbActivateSdmUidReadCounterEncrypted);
        cbReadCounterLimit = getView().findViewById(R.id.cbActivateSdmReadCounterLimit);
        cbEncryptedFileDataMirror = getView().findViewById(R.id.cbActivateSdmEncryptedFileDataMirror);
        etSdmReadCounterLimit = getView().findViewById(R.id.etActivateSdmReadCounterLimit);
        etSdmReadCounterLimitLayout = getView().findViewById(R.id.etActivateSdmAccessReadCounterLimitLayout);
        etSdmAccessRights = getView().findViewById(R.id.etActivateSdmSdmAccessRights);
        etSdmAccessRightsLayout = getView().findViewById(R.id.etActivateSdmAccessSdmAccessRightsLayout);
        etBaseUrl = getView().findViewById(R.id.etActivateSdmBaseUrl);
        etTemplateUrl = getView().findViewById(R.id.etActivateSdmTemplateUrl);

        // hide soft keyboard from showing up on startup
        getActivity().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

        mNfcAdapter = NfcAdapter.getDefaultAdapter(getView().getContext());

        etBaseUrl.setText(NdefForSdm.SAMPLE_BASE_URL);

        showSdmParameter(false);
        clickableSdmParameter(false);

        // get status on what to do
        int checkedRadioButtonId = rgStatus.getCheckedRadioButtonId();
        rgStatus.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup radioGroup, int id) {
                if (id == R.id.rbActivateSdmOn) {
                    Log.d(TAG, "rb Activate On");
                    llUrl.setVisibility(View.VISIBLE);
                    showSdmParameter(true);
                    clickableSdmParameter(true);
                    etSdmAccessRightsLayout.setVisibility(View.VISIBLE);
                    etSdmReadCounterLimitLayout.setVisibility(View.GONE);
                    cbSdmEnabled.setChecked(true);
                    etCommunicationSettings.setText("Plain communication"); // fixed
                    etAccessRights.setText("RW: 0 | CAR: 0 | R: 14 | W:0"); // fixed
                } else if (id == R.id.rbActivateSdmOff) {
                    Log.d(TAG, "rb Activate Off");
                    llUrl.setVisibility(View.GONE);
                    showSdmParameter(false);
                    clickableSdmParameter(false);
                    etSdmAccessRightsLayout.setVisibility(View.GONE);
                    etSdmReadCounterLimitLayout.setVisibility(View.GONE);
                    cbSdmEnabled.setChecked(false);
                    etCommunicationSettings.setText("Plain communication"); // fixed
                    etAccessRights.setText("RW: 0 | CAR: 0 | R: 14 | W:0"); // fixed
                } else if (id == R.id.rbActivateSdmShowStatus) {
                    Log.d(TAG, "rb Show Status");
                    llUrl.setVisibility(View.GONE);
                    showSdmParameter(true);
                    clickableSdmParameter(false);
                    etSdmAccessRightsLayout.setVisibility(View.VISIBLE);
                    cbSdmEnabled.setChecked(false);
                }
            }
        });

        // checking on setReadCounterLimit
        cbReadCounterLimit.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if (cbReadCounterLimit.isChecked()) {
                    etSdmReadCounterLimitLayout.setVisibility(View.VISIBLE);
                    etSdmReadCounterLimit.setVisibility(View.VISIBLE);
                    etSdmReadCounterLimit.setText("16777214");
                    //etSdmReadCounterLimit.setFocusable(true);
                } else {
                    etSdmReadCounterLimitLayout.setVisibility(View.GONE);
                    etSdmReadCounterLimit.setVisibility(View.GONE);
                    etSdmReadCounterLimit.setText("0");
                    //etSdmReadCounterLimit.setFocusable(false);
                }
            }
        });

        // checking on UidReadCounterEncrypted
        cbUidReadCounterEncrypted.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                String metaRight;
                if (cbUidReadCounterEncrypted.isChecked()) {
                    metaRight = "3";
                } else {
                    metaRight = "14";
                }
                StringBuilder sbSdmAccessRights = new StringBuilder();
                sbSdmAccessRights.append("Meta Read: ").append(metaRight);
                sbSdmAccessRights.append(" | File Read: ").append("3");
                sbSdmAccessRights.append(" | Counter Read: ").append("3");
                writeToUi(etSdmAccessRights, sbSdmAccessRights.toString());
                if (cbUidReadCounterEncrypted.isChecked()) {
                    cbUidMirror.setChecked(true);
                    cbReadCounterMirror.setChecked(true);
                    cbUidMirror.setClickable(false);
                    cbReadCounterMirror.setClickable(false);
                } else {
                    cbUidMirror.setChecked(false);
                    cbReadCounterMirror.setChecked(false);
                    cbUidMirror.setClickable(true);
                    cbReadCounterMirror.setClickable(true);
                }
            }
        });
    }

    /**
     * get file settings from tag
     * This is using a fixed applicationId of '0x010000' and a fixed fileId of '0x02'
     * There are 3 steps to get the file settings:
     * 1) select the NDEF application
     * 2) authenticate with Application Master Key
     * 3) get the fileSettings
     */

    private void getFileSettings() {
        // clearOutputFields();
        //
        writeToUiAppend("get FileSettings for fileId 0x02");
        writeToUiAppend("step 1: select application with ID 0x010000");
        boolean success = desfireEv3.selectApplicationByAid(NDEF_APPLICATION_ID);
        byte[] responseData;
        responseData = desfireEv3.getErrorCode();
        if (success) {
            writeToUiAppendBorderColor("selection of the application SUCCESS", COLOR_GREEN);
            //vibrateShort();
        } else {
            writeToUiAppendBorderColor("selection of the application FAILURE with error code: " + EV3.getErrorCode(responseData) + ", aborted", COLOR_RED);
            return;
        }
        //writeToUiAppend("step 2: authenticate with default Application Master Key");

        //writeToUiAppend("step 3: get the file settings for file ID 0x02");
        writeToUiAppend("step 2: get the file settings for file ID 0x02");
        byte[] response = desfireEv3.getFileSettings(NDEF_FILE_ID);
        responseData = desfireEv3.getErrorCode();
        if (response == null) {
            writeToUiAppendBorderColor("get the file settings for file ID 0x02 FAILURE with error code: " + EV3.getErrorCode(responseData) + ", aborted", COLOR_RED);
            return;
        }
        fileSettings = new FileSettings(NDEF_FILE_ID, response);
        writeToUiAppendBorderColor(fileSettings.dump(), COLOR_GREEN);
        vibrateShort();

/*
sample data with enabled SDM
fileNumber: 02
fileType: 0 (Standard)
communicationSettings: 00 (Plain)
accessRights RW | CAR: 00
accessRights R | W: E0
accessRights RW:  0
accessRights CAR: 0
accessRights R:   14
accessRights W:   0
fileSize: 256
non standard fileOption found
sdmFileOption: 40
isSdmEnabled: true
isSdmOptionsBit0_Encode: true
isSdmOptionsBit4_SDMENCFileData: true
isSdmOptionsBit5_SDMReadCtrLimit: false
isSdmOptionsBit6_SDMReadCtr: true
isSdmOptionsBit7_UID: true
SDM_AccessRights: F111
SDM_MetaReadAccessRight: 01
SDM_FileReadAccessRight: 01
SDM_CtrRetAccessRight: 01
optional values depending on bit settings (LSB)
SDM_UIDOffset
SDM_ReadCtrOffset
SDM_PICCDataOffset 2A0000
SDM_MACInputOffset 4F0000
SDM_ENCOffset      4F0000
SDM_ENCLength      200000
SDM_MACOffset      750000
SDM_ReadCtrLimit

sample data with disabled SDM

 */

        // now we analyze the data
        if (fileSettings != null) {
            // communication settings (Plain / MACed / Full)
            String communicationSettings = fileSettings.getCommunicationSettingsName();
            writeToUi(etCommunicationSettings, communicationSettings + " communication");

            // access rights RW || CAR || R || W
            StringBuilder sbAccessRights = new StringBuilder();
            sbAccessRights.append("RW: ").append(fileSettings.getAccessRightsRw());
            sbAccessRights.append(" | CAR: ").append(fileSettings.getAccessRightsCar());
            sbAccessRights.append(" | R: ").append(fileSettings.getAccessRightsR());
            sbAccessRights.append(" | W: ").append(fileSettings.getAccessRightsW());
            writeToUi(etAccessRights, sbAccessRights.toString());

            // SDM enabled
            boolean isSdmEnabled = fileSettings.isSdmEnabled();
            if (isSdmEnabled) {
                cbSdmEnabled.setChecked(true);
                showSdmParameter(true);
            } else {
                cbSdmEnabled.setChecked(false);
                showSdmParameter(false);
            }

            if (isSdmEnabled) {
                // ASCII encode
                cbAsciiEncoding.setChecked(fileSettings.isSdmOptionsBit0_Encode());

                // UID mirror active
                cbUidMirror.setChecked(fileSettings.isSdmOptionsBit7_UID());

                // ReadCounter mirror active
                cbReadCounterMirror.setChecked(fileSettings.isSdmOptionsBit6_SDMReadCtr());

                // ReadCounterLimit active
                cbReadCounterLimit.setChecked(fileSettings.isSdmOptionsBit5_SDMReadCtrLimit());
                if (cbReadCounterLimit.isChecked()) {
                    int readCounterLimit = Utils.intFrom3ByteArrayInversed(fileSettings.getSDM_ReadCtrLimit());
                    etSdmReadCounterLimitLayout.setVisibility(View.VISIBLE);
                    etSdmReadCounterLimit.setVisibility(View.VISIBLE);
                    getActivity().runOnUiThread(() -> {
                        etSdmReadCounterLimit.setText(String.valueOf(readCounterLimit));
                    });
                } else {
                    getActivity().runOnUiThread(() -> {
                        etSdmReadCounterLimitLayout.setVisibility(View.GONE);
                        etSdmReadCounterLimit.setVisibility(View.GONE);
                    });
                }

                // UID and/or Read Counter data Encrypted
                // this option depends on SDMMetaRead access right = 0h..4h -> encrypted [value for NTAG 424 DNA]
                byte sdmMetaReadAccessKey = fileSettings.getSDM_MetaReadAccessRight();
                isEncryptedPiccData = false;
                if (sdmMetaReadAccessKey < (byte) 0x0E) {
                    cbUidReadCounterEncrypted.setChecked(true);
                    isEncryptedPiccData = true;
                } else {
                    cbUidReadCounterEncrypted.setChecked(false);
                    isEncryptedPiccData = false;
                }

                // SDMENC mirror active
                cbEncryptedFileDataMirror.setChecked(fileSettings.isSdmOptionsBit4_SDMENCFileData());

                // SDM access rights Meta Data Read || File Read || Counter Reading
                StringBuilder sbSdmAccessRights = new StringBuilder();
                sbSdmAccessRights.append("Meta Read: ").append(fileSettings.getSDM_MetaReadAccessRight());
                sbSdmAccessRights.append(" | File Read: ").append(fileSettings.getSDM_FileReadAccessRight());
                sbSdmAccessRights.append(" | Counter Read: ").append(fileSettings.getSDM_CtrRetAccessRight());
                writeToUi(etSdmAccessRights, sbSdmAccessRights.toString());

                if (isSdmEnabled) {
                    writeToUiAppend("Secure Dynamic Messages (SDM) / SUN is ENABLED");
                } else {
                    writeToUiAppend("Secure Dynamic Messages (SDM) / SUN is DISABLED");
                }
                ;
            } else {
                // unset all checkboxes and edit text
                //cbAsciiEncoding.setChecked(false);
                cbUidMirror.setChecked(false);
                cbReadCounterMirror.setChecked(false);
                cbReadCounterLimit.setChecked(false);
                cbEncryptedFileDataMirror.setChecked(false);
                //cbAsciiEncoding.setChecked(false);
                writeToUi(etSdmAccessRights, "no rights are set");
            }
        }
    }

    /**
     * Enabling  the SDM/SUN feature
     * This is using a fixed applicationId of '0x010000' and a fixed fileId of '0x02'
     * The method will run an authenticateEv2First command with default Application Master Key (zeroed AES-128 key)
     * <p>
     * steps:
     * 1) select applicationId '0x010000'
     * 2) authenticateFirstEv2 with default Application Master Key (zeroed AES-128 key)
     * 3) change file settings on fileId '0x02' with these parameters
     * - file option byte is set to '0x40' = CommunicationMode.Plain and enabled SDM feature
     * - file access rights are set to '0x00E0' (Read & Write access key: 0, CAR key: 0, Read access key: E (free), Write access key: 0
     * - sdm access rights are set to '0xF3x3' (F = RFU, 3 = SDM Read Counter right, x = Read Meta Data right, 3 = Read File Data right)
     * The Read Meta Data right depends on the option 'Are UID & Read Counter mirroring Encrypted
     * if checked the value is set to '3' else to '14' ('0xE')
     * - sdm options byte is set to a value that reflects the selected options
     * - offset parameters are set to a value that reflects the selected options
     * Limitations:
     * - the ASCII encoding is fixed to true
     * - the size for encrypted file data is limited to 16 bytes (the parameter for the complexUrlBuilder is 2 * 16 = 32)
     * 4) the new template URL is written to the fileId '0x02'
     */

    private void enableSdm() {
        writeToUiAppend("disable the SDM feature for fileId 0x02");
        writeToUiAppend("step 1: select application with ID 0x010000");
        boolean success = desfireEv3.selectApplicationByAid(NDEF_APPLICATION_ID);
        byte[] responseData;
        responseData = desfireEv3.getErrorCode();
        if (success) {
            writeToUiAppendBorderColor("selection of the application SUCCESS", COLOR_GREEN);
            //vibrateShort();
        } else {
            writeToUiAppendBorderColor("selection of the application FAILURE with error code: " + EV3.getErrorCode(responseData) + ", aborted", COLOR_RED);
            return;
        }

        writeToUiAppend("step 2: authenticate with default Application Master Key");
        success = desfireEv3.authenticateAesEv2First(APPLICATION_KEY_MASTER_NUMBER, APPLICATION_KEY_MASTER_AES_DEFAULT);
        responseData = desfireEv3.getErrorCode();
        if (success) {
            writeToUiAppendBorderColor("authenticate with default Application Master Key SUCCESS", COLOR_GREEN);
            //vibrateShort();
        } else {
            writeToUiAppendBorderColor("authenticate with default Application Master Key FAILURE with error code: " + EV3.getErrorCode(responseData) + ", aborted", COLOR_RED);
            return;
        }

        writeToUiAppend("step 3: enabling the SDM feature on fileId 0x02");
        // to get the offsets we are building the template URL right now
        NdefForSdm ndefForSdm = new NdefForSdm(NdefForSdm.SAMPLE_BASE_URL);
        int readCounterLimit = Integer.parseInt(etSdmReadCounterLimit.getText().toString());
        int keySdmMetaRead = 14; // free access, no encrypted data
        if (cbUidReadCounterEncrypted.isChecked()) {
            keySdmMetaRead = 3;
        }
        String templateUrl = ndefForSdm.complexUrlBuilder(DesfireEv3Light.NDEF_FILE_02_NUMBER, NdefForSdm.CommunicationSettings.Plain,
                0, 0, 14, 0, true, cbUidMirror.isChecked(), cbReadCounterMirror.isChecked(),
                cbReadCounterLimit.isChecked(), readCounterLimit, cbEncryptedFileDataMirror.isChecked(), 32,
                true, 3, keySdmMetaRead, 3);
        Log.d(TAG, "templateUrl: " + templateUrl);
        if (TextUtils.isEmpty(templateUrl)) {
            writeToUiAppendBorderColor("building of the Template URL FAILURE, aborted", COLOR_RED);
            return;
        }
        byte[] commandData = ndefForSdm.getCommandData(); // this is the complete data
        Log.d(TAG, printData("commandData", commandData));
        if (commandData == null) {
            writeToUiAppendBorderColor("building of the commandData FAILURE, aborted", COLOR_RED);
            return;
        }
        // enabling the feature
        success = desfireEv3.changeFileSettingsNtag424Dna(DesfireEv3Light.NDEF_FILE_02_NUMBER, commandData);
        if (success) {
            writeToUiAppendBorderColor("enabling the SDM feature on fileId 0x02 SUCCESS", COLOR_GREEN);
            //vibrateShort();
        } else {
            writeToUiAppendBorderColor("enabling the SDM feature on fileId 0x02 FAILURE with error code: " + EV3.getErrorCode(responseData) + ", aborted", COLOR_RED);
            return;
        }

        writeToUiAppend("step 4: write the template URL to fileId 0x02");
        //success = desfireEv3.writeToNdefFile2(templateUrl);
        success = desfireEv3.writeToStandardFileUrlPlain(DesfireEv3Light.NDEF_FILE_02_NUMBER, templateUrl);
        if (success) {
            writeToUiAppendBorderColor("write the template URL to fileId 0x02 SUCCESS", COLOR_GREEN);
            getActivity().runOnUiThread(() -> {
                etTemplateUrl.setText(templateUrl);
            });
            vibrateShort();
        } else {
            writeToUiAppendBorderColor("write the template URL to fileId 0x02 FAILURE with error code: " + EV3.getErrorCode(responseData) + ", aborted", COLOR_RED);
            return;
        }
    }


    /**
     * Disabling the SDM/SUN feature
     * This is using a fixed applicationId of '0x010000' and a fixed fileId of '0x02'
     * The method will run an authenticateEv2First command with default Application Master Key (zeroed AES-128 key)
     * <p>
     * steps:
     * 1) select applicationId '0x010000'
     * 2) authenticateFirstEv2 with default Application Master Key (zeroed AES-128 key)
     * 3) change file settings on fileId '0x02' with these parameters
     * - file option byte is set to '0x00' = CommunicationMode.Plain and disabled SDM feature
     * - file access rights are set to '0x00E0' (Read & Write access key: 0, CAR key: 0, Read access key: E (free), Write access key: 0
     */
    private void disableSdm() {
        writeToUiAppend("disable the SDM feature for fileId 0x02");
        writeToUiAppend("step 1: select application with ID 0x010000");
        boolean success = desfireEv3.selectApplicationByAid(NDEF_APPLICATION_ID);
        byte[] responseData;
        responseData = desfireEv3.getErrorCode();
        if (success) {
            writeToUiAppendBorderColor("selection of the application SUCCESS", COLOR_GREEN);
            //vibrateShort();
        } else {
            writeToUiAppendBorderColor("selection of the application FAILURE with error code: " + EV3.getErrorCode(responseData) + ", aborted", COLOR_RED);
            return;
        }

        writeToUiAppend("step 2: authenticate with default Application Master Key");
        success = desfireEv3.authenticateAesEv2First(APPLICATION_KEY_MASTER_NUMBER, APPLICATION_KEY_MASTER_AES_DEFAULT);
        responseData = desfireEv3.getErrorCode();
        if (success) {
            writeToUiAppendBorderColor("authenticate with default Application Master Key SUCCESS", COLOR_GREEN);
            //vibrateShort();
        } else {
            writeToUiAppendBorderColor("authenticate with default Application Master Key FAILURE with error code: " + EV3.getErrorCode(responseData) + ", aborted", COLOR_RED);
            return;
        }

        writeToUiAppend("step 3: disabling the SDM feature on fileId 0x02");
        success = desfireEv3.changeFileSettingsNtag424Dna(NDEF_FILE_ID, DesfireEv3Light.CommunicationSettings.Plain, 0, 0, 14, 0, false, 0, 0, 0);
        responseData = desfireEv3.getErrorCode();
        if (success) {
            writeToUiAppendBorderColor("disabling the SDM feature on fileId 0x02 SUCCESS", COLOR_GREEN);
            vibrateShort();
        } else {
            writeToUiAppendBorderColor("disabling the SDM feature on fileId 0x02 FAILURE with error code: " + EV3.getErrorCode(responseData) + ", aborted", COLOR_RED);
            return;
        }
    }

    /**
     * changes the visibility of SDM parameter
     *
     * @param isShowSdmParameter
     */
    private void showSdmParameter(boolean isShowSdmParameter) {
        getActivity().runOnUiThread(() -> {
            int visibility = View.GONE;
            if (isShowSdmParameter) visibility = View.VISIBLE;
            cbAsciiEncoding.setVisibility(visibility);
            cbUidMirror.setVisibility(visibility);
            cbReadCounterMirror.setVisibility(visibility);
            cbUidReadCounterEncrypted.setVisibility(visibility);
            cbReadCounterLimit.setVisibility(visibility);
            cbEncryptedFileDataMirror.setVisibility(visibility);
            //etSdmReadCounterLimitLayout.setVisibility(visibility); // visibility is set depending of cbReadCounterLimit
            //etSdmReadCounterLimit.setVisibility(visibility); // visibility is set depending of cbReadCounterLimit
            etSdmAccessRights.setVisibility(visibility);
        });
    }

    /**
     * changes the click ability of SDM parameter
     *
     * @param isClickableSdmParameter
     */
    private void clickableSdmParameter(boolean isClickableSdmParameter) {
        getActivity().runOnUiThread(() -> {
            boolean clickable = false;
            if (isClickableSdmParameter) clickable = true;
            //cbAsciiEncoding.setClickable(clickable); // this needs to be enabled
            cbUidMirror.setClickable(clickable);
            cbReadCounterMirror.setClickable(clickable);
            cbUidReadCounterEncrypted.setClickable(clickable);
            cbReadCounterLimit.setClickable(clickable);
            cbEncryptedFileDataMirror.setClickable(clickable);
            //etSdmReadCounterLimitLayout.setFocusable(clickable);
            //etSdmReadCounterLimit.setFocusable(clickable);
            etSdmAccessRights.setFocusable(clickable);
        });
    }
    
    /**
     * section for NFC
     */

    @Override
    public void onTagDiscovered(Tag tag) {
        clearOutputFields();
        writeToUiAppend("NFC tag discovered");
        isoDep = null;
        try {
            isoDep = IsoDep.get(tag);
            if (isoDep != null) {
                // Make a Vibration
                vibrateShort();

                getActivity().runOnUiThread(() -> {
                    output.setText("");
                    output.setBackgroundColor(getResources().getColor(R.color.white));
                });
                isoDep.connect();
                if (!isoDep.isConnected()) {
                    writeToUiAppendBorderColor("could not connect to the tag, aborted", COLOR_RED);
                    isoDep.close();
                    return;
                }
                desfireEv3 = new DesfireEv3Light(isoDep);
                isDesfireEv3 = desfireEv3.checkForDESFireEv3();
                if (!isDesfireEv3) {
                    writeToUiAppendBorderColor("The tag is not a DESFire EV3 tag, stopping any further activities", COLOR_RED);
                    return;
                }

                // get tag ID
                tagIdByte = tag.getId();
                writeToUiAppend("tag id: " + Utils.bytesToHex(tagIdByte));
                Log.d(TAG, "tag id: " + Utils.bytesToHex(tagIdByte));
                writeToUiAppendBorderColor("The app and DESFire EV3 tag are ready to use", COLOR_GREEN);

                if (rbActivateSdmGetStatus.isChecked()) {
                    getFileSettings();
                }
                if (rbActivateSdmOn.isChecked()) {
                    enableSdm();
                }
                if (rbActivateSdmOff.isChecked()) {
                    disableSdm();
                }
            }
        } catch (IOException e) {
            writeToUiAppendBorderColor("IOException: " + e.getMessage(), COLOR_RED);
            e.printStackTrace();
        } catch (Exception e) {
            writeToUiAppendBorderColor("Exception: " + e.getMessage(), COLOR_RED);
            e.printStackTrace();
        }
    }
 
    /**
     * section for UI service methods
     */

    private void writeToUiAppend(String message) {
        writeToUiAppend(output, message);
    }
    private void writeToUiAppend(TextView textView, String message) {
        getActivity().runOnUiThread(() -> {
            String oldString = textView.getText().toString();
            if (TextUtils.isEmpty(oldString)) {
                textView.setText(message);
            } else {
                String newString = message + "\n" + oldString;
                textView.setText(newString);
                System.out.println(message);
            }
        });
    }

    private void writeToUi(TextView textView, String message) {
        getActivity().runOnUiThread(() -> {
            textView.setText(message);
        });
    }

    private void writeToUiAppendBorderColor(String message, int color) {
        writeToUiAppendBorderColor(output, outputLayout, message, color);
    }

    private void writeToUiAppendBorderColor(TextView textView, TextInputLayout textInputLayout, String message, int color) {
        getActivity().runOnUiThread(() -> {

            // set the color to green
            //Color from rgb
            // int color = Color.rgb(255,0,0); // red
            //int color = Color.rgb(0,255,0); // green
            //Color from hex string
            //int color2 = Color.parseColor("#FF11AA"); light blue
            int[][] states = new int[][]{
                    new int[]{android.R.attr.state_focused}, // focused
                    new int[]{android.R.attr.state_hovered}, // hovered
                    new int[]{android.R.attr.state_enabled}, // enabled
                    new int[]{}  //
            };
            int[] colors = new int[]{
                    color,
                    color,
                    color,
                    //color2
                    color
            };
            ColorStateList myColorList = new ColorStateList(states, colors);
            textInputLayout.setBoxStrokeColorStateList(myColorList);

            String oldString = textView.getText().toString();
            if (TextUtils.isEmpty(oldString)) {
                textView.setText(message);
            } else {
                String newString = message + "\n" + oldString;
                textView.setText(newString);
                System.out.println(message);
            }
        });
    }

    private void clearOutputFields() {
        getActivity().runOnUiThread(() -> {
            output.setText("");
        });
        // reset the border color to primary for errorCode
        int color = R.color.colorPrimary;
        int[][] states = new int[][]{
                new int[]{android.R.attr.state_focused}, // focused
                new int[]{android.R.attr.state_hovered}, // hovered
                new int[]{android.R.attr.state_enabled}, // enabled
                new int[]{}  //
        };
        int[] colors = new int[]{
                color,
                color,
                color,
                color
        };
        ColorStateList myColorList = new ColorStateList(states, colors);
        outputLayout.setBoxStrokeColorStateList(myColorList);
    }

    private void vibrateShort() {
        // Make a Sound
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ((Vibrator) getActivity().getSystemService(VIBRATOR_SERVICE)).vibrate(VibrationEffect.createOneShot(50, 10));
        } else {
            Vibrator v = (Vibrator) getActivity().getSystemService(VIBRATOR_SERVICE);
            v.vibrate(50);
        }
    }

    private void showWirelessSettings() {
        Toast.makeText(getView().getContext(), "You need to enable NFC", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(Settings.ACTION_WIRELESS_SETTINGS);
        startActivity(intent);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mNfcAdapter != null) {

            if (!mNfcAdapter.isEnabled())
                showWirelessSettings();

            Bundle options = new Bundle();
            // Work around for some broken Nfc firmware implementations that poll the card too fast
            options.putInt(NfcAdapter.EXTRA_READER_PRESENCE_CHECK_DELAY, 250);

            // Enable ReaderMode for all types of card and disable platform sounds
            // the option NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK is NOT set
            // to get the data of the tag afer reading
            mNfcAdapter.enableReaderMode(getActivity(),
                    this,
                    NfcAdapter.FLAG_READER_NFC_A |
                            NfcAdapter.FLAG_READER_NFC_B |
                            NfcAdapter.FLAG_READER_NFC_F |
                            NfcAdapter.FLAG_READER_NFC_V |
                            NfcAdapter.FLAG_READER_NFC_BARCODE |
                            NfcAdapter.FLAG_READER_NO_PLATFORM_SOUNDS,
                    options);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mNfcAdapter != null)
            mNfcAdapter.disableReaderMode(getActivity());
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }
}