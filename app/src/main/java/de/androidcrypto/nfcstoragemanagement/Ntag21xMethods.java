package de.androidcrypto.nfcstoragemanagement;

import static de.androidcrypto.nfcstoragemanagement.Utils.testBit;

import android.nfc.TagLostException;
import android.nfc.tech.NfcA;
import android.text.TextUtils;
import android.widget.TextView;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

/**
 * The class takes all interactions with a NTAG21x tag
 */

public class Ntag21xMethods {

    private TextView textView; // used for displaying informations from the functions

    public Ntag21xMethods(TextView textView) {
        this.textView = textView;
    }

    /**
     * read the content of the user memory upto 'numberOfBytes' - this is because the maximum NDEF length
     * got defined in NdefSettingsFragment
     * The content is used to find matching strings with UID and/or MAC
     * Note: if any mirror is enabled on the tag the returned content is the VIRTUAL content including
     * the mirror content, not the REAL content written in pages !
     *
     * @param nfcA
     * @param numberOfBytesToRead
     * @return
     */
    public byte[] readNdefContent(NfcA nfcA, int numberOfBytesToRead, int nfcaMaxTranceiveLength ) {
        int nfcaMaxTranceive4ByteTrunc = nfcaMaxTranceiveLength / 4; // 63
        int nfcaMaxTranceive4ByteLength = nfcaMaxTranceive4ByteTrunc * 4; // 252 bytes
        int nfcaNrOfFullReadings = numberOfBytesToRead / nfcaMaxTranceive4ByteLength;
        int nfcaTotalFullReadingBytes = nfcaNrOfFullReadings * nfcaMaxTranceive4ByteLength; // 3 * 252 = 756
        int nfcaMaxTranceiveModuloLength = numberOfBytesToRead - nfcaTotalFullReadingBytes; // 888 bytes - 756 bytes = 132 bytes
        String nfcaContent = "";
        nfcaContent = nfcaContent + "nfcaMaxTranceive4ByteTrunc: " + nfcaMaxTranceive4ByteTrunc + "\n";
        nfcaContent = nfcaContent + "nfcaMaxTranceive4ByteLength: " + nfcaMaxTranceive4ByteLength + "\n";
        nfcaContent = nfcaContent + "nfcaNrOfFullReadings: " + nfcaNrOfFullReadings + "\n";
        nfcaContent = nfcaContent + "nfcaTotalFullReadingBytes: " + nfcaTotalFullReadingBytes + "\n";
        nfcaContent = nfcaContent + "nfcaMaxTranceiveModuloLength: " + nfcaMaxTranceiveModuloLength + "\n";

        byte[] response;
        byte[] ntagMemory = new byte[numberOfBytesToRead];
        try {
            // first round
            for (int i = 0; i < nfcaNrOfFullReadings; i++) {
                //nfcaContent = nfcaContent + "starting round: " + i + "\n";
                System.out.println("starting round: " + i);
                byte[] commandF = new byte[]{
                        (byte) 0x3A,  // FAST_READ
                        (byte) ((4 + (nfcaMaxTranceive4ByteTrunc * i)) & 0x0ff), // page 4 is the first user memory page
                        (byte) ((4 + (nfcaMaxTranceive4ByteTrunc * (i + 1)) - 1) & 0x0ff)
                };
                //nfcaContent = nfcaContent + "i: " + i + " commandF: " + bytesToHex(commandF) + "\n";
                response = nfcA.transceive(commandF);
                if (response == null) {
                    // either communication to the tag was lost or a NACK was received
                    // Log and return
                    nfcaContent = nfcaContent + "ERROR: null response";
                    String finalNfcaText = nfcaContent;
                    writeToUiAppend(textView, finalNfcaText);
                    System.out.println(finalNfcaText);
                    return null;
                } else if ((response.length == 1) && ((response[0] & 0x00A) != 0x00A)) {
                    // NACK response according to Digital Protocol/T2TOP
                    // Log and return
                    nfcaContent = nfcaContent + "ERROR: NACK response: " + Utils.bytesToHexNpe(response);
                    String finalNfcaText = nfcaContent;
                    writeToUiAppend(textView, finalNfcaText);
                    System.out.println(finalNfcaText);
                    return null;
                } else {
                    // success: response contains ACK or actual data
                    System.arraycopy(response, 0, ntagMemory, (nfcaMaxTranceive4ByteLength * i), nfcaMaxTranceive4ByteLength);
                }
            } // for
            // now we read the nfcaMaxTranceiveModuloLength bytes, for a NTAG216 = 132 bytes
            //nfcaContent = nfcaContent + "starting last round: " + "\n";
            //System.out.println("starting last round: ");
            byte[] commandF = new byte[]{
                    (byte) 0x3A,  // FAST_READ
                    (byte) ((4 + (nfcaMaxTranceive4ByteTrunc * nfcaNrOfFullReadings)) & 0x0ff), // page 4 is the first user memory page
                    (byte) ((4 + (nfcaMaxTranceive4ByteTrunc * nfcaNrOfFullReadings) + (nfcaMaxTranceiveModuloLength / 4) & 0x0ff))
            };
            //nfcaContent = nfcaContent + "last: " + " commandF: " + bytesToHex(commandF) + "\n";
            response = nfcA.transceive(commandF);
            if (response == null) {
                // either communication to the tag was lost or a NACK was received
                // Log and return
                nfcaContent = nfcaContent + "ERROR: null response";
                String finalNfcaText = nfcaContent;
                writeToUiAppend(textView, finalNfcaText);
                System.out.println(finalNfcaText);
                return null;
            } else if ((response.length == 1) && ((response[0] & 0x00A) != 0x00A)) {
                // NACK response according to Digital Protocol/T2TOP
                // Log and return
                nfcaContent = nfcaContent + "ERROR: NACK response: " + Utils.bytesToHexNpe(response);
                String finalNfcaText = nfcaContent;
                //writeToUiAppend(resultNfcWriting, finalNfcaText);
                System.out.println(finalNfcaText);
                return null;
            } else {
                // success: response contains ACK or actual data
                System.arraycopy(response, 0, ntagMemory, (nfcaMaxTranceive4ByteLength * nfcaNrOfFullReadings), nfcaMaxTranceiveModuloLength);
            }
        } catch (IOException e) {
            writeToUiAppend(textView, "ERROR: IOException " + e.toString());
            e.printStackTrace();
            return null;
        }
        return ntagMemory;
    }

    /**
     * The bit for enabling or disabling the uid mirror is in page 41/131/227 (0x29/0x83/0xE3),
     * depending on the tag type
     *
     * byte 0 of this pages holds the MIRROR byte
     * byte 2 of this pages holds the MIRROR_PAGE byte
     *
     * Mirror byte has these flags
     * bits 6+7 define which mirror shall be used:
     *   00b = no ASCII mirror
     *   01b = Uid ASCII mirror
     *   10b = NFC counter ASCII mirror
     *   11b = Uid and NFC counter ASCII mirror
     * bits 4+5 define the byte position within the page defined in MIRROR_PAGE byte
     *
     * MIRROR_PAGE byte defines the start of mirror.
     *
     * It is import that the end of mirror is within the user memory. These lengths apply:
     * Uid mirror: 14 bytes (UID length is 7 * 2 for hex ascii string encoding)
     * NFC counter mirror: 6 bytes (counter length is 3 * 2 for hex ascii string encoding)
     * Uid + NFC counter mirror: 21 bytes (14 bytes for Uid and 1 byte separation + 6 bytes counter value
     * Separator is 'x' (0x78)
     *
     */

    /**
     * Checks the enabled or disabled UID mirroring on the tag and returns the mirror position
     * in the user memory
     * @param nfcA
     * @param pageOfConfiguration page in ntag where configuration is starting, depending on NTAG sub type 213/215/216
     * @return uid mirror position
     * return is -1 if uid mirror is disabled or errors applied in parameters
     */
    private int checkUidMirrorStatus(NfcA nfcA, int pageOfConfiguration) {
        // sanity checks
        if ((nfcA == null) || (!nfcA.isConnected())) {
            writeToUiAppend(textView, "NfcA is not available for reading, aborted");
            return -1;
        }
        if ((pageOfConfiguration != 41) && (pageOfConfiguration != 131) && (pageOfConfiguration != 227)) {
            writeToUiAppend(textView, "wrong parameter for pageOfConfiguration, aborted");
            return -1;
        }
        // read configuration page 0
        byte[] readPageResponse = getTagDataResponse(nfcA, pageOfConfiguration);
        int position = -1;
        if ((readPageResponse != null) && (readPageResponse.length > 2)) {
            // get byte 0 = MIRROR
            byte mirrorByte = readPageResponse[0];
            // get byte 2 = MIRROR_PAGE
            byte mirrorPageByte = readPageResponse[2];
            writeToUiAppend(textView, "mirrorPageByte: " + Utils.byteToHex(mirrorPageByte) + " (= " + (int) mirrorPageByte + " dec)");
            writeToUiAppend(textView, "MIRROR content old: " + Utils.printByteBinary(mirrorByte));
            // check that ONLY the UID mirror is active, NOT the COUNTER mirror
            boolean isUidMirror, isCounterMirror, isMirrorBit4, isMirrorBit5;
            isUidMirror = testBit(mirrorByte, 6);
            isCounterMirror = testBit(mirrorByte, 7);
            isMirrorBit4 = testBit(mirrorByte, 4);
            isMirrorBit5 = testBit(mirrorByte, 5);
            writeToUiAppend(textView, "isUidMirror: " + isUidMirror + " || isCounterMirror: " + isCounterMirror);
            writeToUiAppend(textView, "isMirrorBit4: " + isMirrorBit4 + " || isMirrorBit5: " + isMirrorBit5);
            if (isCounterMirror) {
                writeToUiAppend(textView, "the COUNTER mirror is enabled, aborted");
                return position;
            }
            if (!isUidMirror) {
                writeToUiAppend(textView, "the UID mirror is disabled, aborted");
                return position;
            }
            // at this point only the UID mirror is enabled, find the relative position in user memory
            position = ((int) mirrorPageByte - 4) * 4; // 4 header pages are subtracted
            // now we need to add the position within the page, this is encoded in mirrorByte -> mirrorBits 4 + 5
            position += (isMirrorBit4 ? 1 : 0) + (isMirrorBit5 ? 2 : 0);
            return position;
        } else {
            writeToUiAppend(textView, "something gone wrong, aborted");
            return position;
        }
    }

    /**
     * enables the UID mirror and sets the mirror position
     * if Counter mirror should be enabled before it gets disabled
     * @param nfcA
     * @param pageOfConfiguration
     * @param positionOfUid : the relative position within the user memory, so starting with 0
     * @return true on success
     */

    private boolean enableUidMirror(NfcA nfcA, int pageOfConfiguration, int positionOfUid) {
        // sanity checks
        if ((nfcA == null) || (!nfcA.isConnected())) {
            writeToUiAppend(textView, "NfcA is not available for reading, aborted");
            return false;
        }
        if ((pageOfConfiguration != 41) && (pageOfConfiguration != 131) && (pageOfConfiguration != 227)) {
            writeToUiAppend(textView, "wrong parameter for pageOfConfiguration, aborted");
            return false;
        }
        // read configuration page 0
        byte[] readPageResponse = getTagDataResponse(nfcA, pageOfConfiguration);
        int position = -1;
        if ((readPageResponse != null) && (readPageResponse.length > 2)) {
            // get byte 0 = MIRROR
            byte mirrorByte = readPageResponse[0];
            // get byte 2 = MIRROR_PAGE
            byte mirrorPageByte = readPageResponse[2];
            writeToUiAppend(textView, "mirrorPageByte: " + Utils.byteToHex(mirrorPageByte) + " (= " + (int) mirrorPageByte + " dec)");
            writeToUiAppend(textView, "MIRROR content old: " + Utils.printByteBinary(mirrorByte));

            // this will activate the UID mirror and DEactivate an activated Counter mirror
            byte mirrorByteNew;
            // unsetting bit 7 = counter, we are doing UID mirroring only
            mirrorByteNew = Utils.unsetBitInByte(mirrorByte, 7);
            // setting bit 6
            mirrorByteNew = Utils.setBitInByte(mirrorByteNew, 6);

            // now we are converting the relative position of MAC mirror in 'page' and 'position in page'
            int newMirrorPage = 4 + (positionOfUid / 4); // NTAG 21x has 4 header pages
            writeToUiAppend(textView, "newPage: " + newMirrorPage);
            int positionInPage = (positionOfUid / 4) - (newMirrorPage - 4);
            writeToUiAppend(textView, "positionInPage: " + positionInPage);
            // set the bits depending on positionÍnPage - this could be more elegant but...
            if (positionInPage == 0) {
                mirrorByteNew = Utils.unsetBitInByte(mirrorByteNew, 4);
                mirrorByteNew = Utils.unsetBitInByte(mirrorByteNew, 5);
            } else if (positionInPage == 1) {
                mirrorByteNew = Utils.setBitInByte(mirrorByteNew, 4);
                mirrorByteNew = Utils.unsetBitInByte(mirrorByteNew, 5);
            } else if (positionInPage == 2) {
                mirrorByteNew = Utils.unsetBitInByte(mirrorByteNew, 4);
                mirrorByteNew = Utils.setBitInByte(mirrorByteNew, 5);
            } else if (positionInPage == 3) {
                mirrorByteNew = Utils.setBitInByte(mirrorByteNew, 4);
                mirrorByteNew = Utils.setBitInByte(mirrorByteNew, 5);
            }
            // as the formula result in values of 0..3 we do not need a final 'else'
            // this is setting the full page
            byte mirrorPageByteNew = (byte) (newMirrorPage & 0x0ff);
            // now copy the new contents to readResponse
            readPageResponse[0] = mirrorByteNew;
            readPageResponse[2] = mirrorPageByteNew;
            writeToUiAppend(textView, "readPageResponse: " + Utils.bytesToHexNpe(readPageResponse));
            // write the page back to the tag
            byte[] writePageResponse = writeTagDataResponse(nfcA, pageOfConfiguration, readPageResponse);
            writeToUiAppend(textView, "write page to tag: " + Utils.bytesToHexNpe(writePageResponse));
            if (writePageResponse != null) {
                writeToUiAppend(textView, "SUCCESS: enabling the UID mirror with response: " + Utils.bytesToHexNpe(writePageResponse));
                return true;
            } else {
                writeToUiAppend(textView, "FAILURE: no enabling of the UID mirror");
                return false;
            }
        } else {
            writeToUiAppend(textView, "something gone wrong, aborted");
            return false;
        }
    }

    /**
     * disables ALL mirrors whether they are set or not and resets the position to facory settings
     * @param nfcA
     * @param pageOfConfiguration
     * @return true on success
     */

    private boolean disableAllMirror(NfcA nfcA, int pageOfConfiguration) {
        // sanity checks
        if ((nfcA == null) || (!nfcA.isConnected())) {
            writeToUiAppend(textView, "NfcA is not available for reading, aborted");
            return false;
        }
        if ((pageOfConfiguration != 41) && (pageOfConfiguration != 131) && (pageOfConfiguration != 227)) {
            writeToUiAppend(textView, "wrong parameter for pageOfConfiguration, aborted");
            return false;
        }
        // read configuration page 0
        byte[] readPageResponse = getTagDataResponse(nfcA, pageOfConfiguration);
        int position = -1;
        if ((readPageResponse != null) && (readPageResponse.length > 2)) {
            // get byte 0 = MIRROR
            byte mirrorByte = readPageResponse[0];
            // get byte 2 = MIRROR_PAGE
            byte mirrorPageByte = readPageResponse[2];
            writeToUiAppend(textView, "mirrorPageByte: " + Utils.byteToHex(mirrorPageByte) + " (= " + (int) mirrorPageByte + " dec)");
            writeToUiAppend(textView, "MIRROR content old: " + Utils.printByteBinary(mirrorByte));

            // this will DEactivate the UID mirror and DEactivate an activated Counter mirror as well
            byte mirrorByteNew;
            // unsetting bit 7 = counter, we are doing UID mirroring only
            mirrorByteNew = Utils.unsetBitInByte(mirrorByte, 7);
            // setting bit 6
            mirrorByteNew = Utils.unsetBitInByte(mirrorByteNew, 6);

            // the mirror position is reset to page 0 (outside the user memory) and positionInPage 0
            mirrorByteNew = Utils.unsetBitInByte(mirrorByteNew, 4);
            mirrorByteNew = Utils.unsetBitInByte(mirrorByteNew, 5);
            byte mirrorPageByteNew = (byte) (0x00);
            // now copy the new contents to readResponse
            readPageResponse[0] = mirrorByteNew;
            readPageResponse[2] = mirrorPageByteNew;
            // write the page back to the tag
            byte[] writePageResponse = writeTagDataResponse(nfcA, pageOfConfiguration, readPageResponse);
            writeToUiAppend(textView, "write page to tag: " + Utils.bytesToHexNpe(writePageResponse));
            if (writePageResponse != null) {
                writeToUiAppend(textView, "SUCCESS: enabling the UID mirror with response: " + Utils.bytesToHexNpe(writePageResponse));
                return true;
            } else {
                writeToUiAppend(textView, "FAILURE: no enabling of the UID mirror");
                return false;
            }
        } else {
            writeToUiAppend(textView, "something gone wrong, aborted");
            return false;
        }
    }


    public boolean writeMacToNdef(NfcA nfcA, int pageOfConfiguration, byte[] shortenedMac, int macPosition) {
        // sanity checks
        if ((nfcA == null) || (!nfcA.isConnected())) {
            writeToUiAppend(textView, "NfcA is not available for reading, aborted");
            return false;
        }
        if ((pageOfConfiguration != 41) && (pageOfConfiguration != 131) && (pageOfConfiguration != 227)) {
            writeToUiAppend(textView, "wrong parameter for pageOfConfiguration, aborted");
            return false;
        }
        if ((shortenedMac == null) || (shortenedMac.length != 8)) {
            writeToUiAppend(textView, "wrong parameter for shortenedMac, aborted");
            return false;
        }
        if ((macPosition < 6)) {
            writeToUiAppend(textView, "wrong parameter for macPosition, aborted");
            return false;
        }

        // read configuration page 0
        byte[] readPageResponse = getTagDataResponse(nfcA, pageOfConfiguration);
        int position = -1;
        if ((readPageResponse != null) && (readPageResponse.length > 2)) {
            // get byte 0 = MIRROR
            byte mirrorByte = readPageResponse[0];
            // get byte 2 = MIRROR_PAGE
            byte mirrorPageByte = readPageResponse[2];
            writeToUiAppend(textView, "mirrorPageByte: " + Utils.byteToHex(mirrorPageByte) + " (= " + (int) mirrorPageByte + " dec)");
            writeToUiAppend(textView, "MIRROR content old: " + Utils.printByteBinary(mirrorByte));

            // this will activate the UID mirror and DEactivate an activated Counter mirror
            byte mirrorByteNew;
            // unsetting bit 7 = counter, we are doing UID mirroring only
            mirrorByteNew = Utils.unsetBitInByte(mirrorByte, 7);
            // setting bit 6
            mirrorByteNew = Utils.setBitInByte(mirrorByteNew, 6);

            // now we are converting the relative position of MAC mirror in 'page' and 'position in page'
            int newMacPage = 4 + (macPosition / 4); // NTAG 21x has 4 header pages
            writeToUiAppend(textView, "newMacPage: " + newMacPage);
            int positionInPage = (macPosition / 4) - (newMacPage - 4);
            writeToUiAppend(textView, "positionInPage: " + positionInPage);
            // we can write only 8 bytes in on writing we need to split up the 8 bytes shortenedMac
            boolean result = false;
            if (positionInPage == 0) {
                // the easiest option, write the first 4 bytes in the page and the next 4 bytes in the following page
                result = writeTagDataResponseBoolean(nfcA, newMacPage, Arrays.copyOfRange(shortenedMac, 0, 4));
                if (result) {
                    newMacPage ++;
                    result = writeTagDataResponseBoolean(nfcA, newMacPage, Arrays.copyOfRange(shortenedMac, 4, 8));
                    if (result) return true;
                }
            } else if (positionInPage == 1) {
                // write the first 3 bytes to page (but read the first byte to not overwrite the data)
                // then write 4 bytes to the next page and write 1 byte in the over next page
                byte[] readPage = getTagDataResponse(nfcA, newMacPage);
                System.arraycopy(Arrays.copyOfRange(shortenedMac, 0, 3), 0, readPage, 1, 3);
                result = writeTagDataResponseBoolean(nfcA, newMacPage, readPage); // 3 mac bytes
                if (result) {
                    newMacPage ++;
                    result = writeTagDataResponseBoolean(nfcA, newMacPage, Arrays.copyOfRange(shortenedMac, 3, 7)); // 4 mac bytes
                    if (result) {
                        newMacPage ++;
                        readPage = getTagDataResponse(nfcA, newMacPage);
                        System.arraycopy(Arrays.copyOfRange(shortenedMac, 7, 8), 0, readPage, 0, 1); // 1 mac byte
                        result = writeTagDataResponseBoolean(nfcA, newMacPage, readPage); // 3 mac bytes
                        if (result) return true;
                    }
                }
            } else if (positionInPage == 2) {
                // write the first 2 bytes to page (but read the two first bytes to not overwrite the data)
                // then write 4 bytes to the next page and write 2 byte in the over next page
                byte[] readPage = getTagDataResponse(nfcA, newMacPage);
                System.arraycopy(Arrays.copyOfRange(shortenedMac, 0, 2), 0, readPage, 2, 2);
                result = writeTagDataResponseBoolean(nfcA, newMacPage, readPage); // 2 mac bytes
                if (result) {
                    newMacPage ++;
                    result = writeTagDataResponseBoolean(nfcA, newMacPage, Arrays.copyOfRange(shortenedMac, 2, 6)); // 4 mac bytes
                    if (result) {
                        newMacPage ++;
                        readPage = getTagDataResponse(nfcA, newMacPage);
                        System.arraycopy(Arrays.copyOfRange(shortenedMac, 6, 8), 0, readPage, 0, 2); // 2 mac bytes
                        result = writeTagDataResponseBoolean(nfcA, newMacPage, readPage); // 2 mac bytes
                        if (result) return true;
                    }
                }
            } else if (positionInPage == 3) {
                // write the first 1 bytes to page (but read the three first bytes to not overwrite the data)
                // then write 4 bytes to the next page and write 3 bytes in the over next page
                byte[] readPage = getTagDataResponse(nfcA, newMacPage);
                System.arraycopy(Arrays.copyOfRange(shortenedMac, 0, 1), 0, readPage, 3, 1);
                result = writeTagDataResponseBoolean(nfcA, newMacPage, readPage); // 1 mac byte
                if (result) {
                    newMacPage ++;
                    result = writeTagDataResponseBoolean(nfcA, newMacPage, Arrays.copyOfRange(shortenedMac, 1, 5)); // 4 mac bytes
                    if (result) {
                        newMacPage ++;
                        readPage = getTagDataResponse(nfcA, newMacPage);
                        System.arraycopy(Arrays.copyOfRange(shortenedMac, 5, 8), 0, readPage, 0, 3); // 3 mac bytes
                        result = writeTagDataResponseBoolean(nfcA, newMacPage, readPage); // 3 mac bytes
                        if (result) return true;
                    }
                }
            }
            return false;
        } else {
            writeToUiAppend(textView, "something gone wrong, aborted");
            return false;
        }
    }

    private byte[] getTagDataResponse(NfcA nfcA, int page) {
        byte[] response;
        byte[] command = new byte[]{
                (byte) 0x30,  // READ
                (byte) (page & 0x0ff), // page 0
        };
        try {
            response = nfcA.transceive(command); // response should be 16 bytes = 4 pages
            if (response == null) {
                // either communication to the tag was lost or a NACK was received
                writeToUiAppend(textView, "Error on reading page " + page);
                return null;
            } else if ((response.length == 1) && ((response[0] & 0x00A) != 0x00A)) {
                // NACK response according to Digital Protocol/T2TOP
                // Log and return
                writeToUiAppend(textView, "Error (NACK) on reading page " + page);
                return null;
            } else {
                // success: response contains ACK or actual data
                writeToUiAppend(textView, "SUCCESS on reading page " + page + " response: " + Utils.bytesToHexNpe(response));
                System.out.println("reading page " + page + ": " + Utils.bytesToHexNpe(response));
            }
        } catch (TagLostException e) {
            // Log and return
            writeToUiAppend(textView, "ERROR: Tag lost exception on reading");
            return null;
        } catch (IOException e) {
            writeToUiAppend(textView, "ERROR: IOEexception: " + e);
            e.printStackTrace();
            return null;
        }
        return response;
    }

    private boolean writeTagDataResponseBoolean(NfcA nfcA, int page, byte[] dataByte) {
        byte[] writePageResponse = writeTagDataResponse(nfcA, page, dataByte);
        writeToUiAppend(textView, "write page to tag: " + Utils.bytesToHexNpe(writePageResponse));
        if (writePageResponse != null) {
            writeToUiAppend(textView, "SUCCESS: enabling the UID mirror with response: " + Utils.bytesToHexNpe(writePageResponse));
            return true;
        } else {
            writeToUiAppend(textView, "FAILURE: no enabling of the UID mirror");
            return false;
        }
    }

    private byte[] writeTagDataResponse(NfcA nfcA, int page, byte[] dataByte) {
        byte[] response;
        byte[] command = new byte[]{
                (byte) 0xA2,  // WRITE
                (byte) (page & 0x0ff),
                dataByte[0],
                dataByte[1],
                dataByte[2],
                dataByte[3]
        };
        try {
            response = nfcA.transceive(command); // response should be 16 bytes = 4 pages
            if (response == null) {
                // either communication to the tag was lost or a NACK was received
                writeToUiAppend(textView, "Error on writing page " + page);
                return null;
            } else if ((response.length == 1) && ((response[0] & 0x00A) != 0x00A)) {
                // NACK response according to Digital Protocol/T2TOP
                // Log and return
                writeToUiAppend(textView, "Error (NACK) on writing page " + page);
                return null;
            } else {
                // success: response contains ACK or actual data
                writeToUiAppend(textView, "SUCCESS on writing page " + page + " response: " + Utils.bytesToHexNpe(response));
                System.out.println("response page " + page + ": " + Utils.bytesToHexNpe(response));
                return response;
            }
        } catch (TagLostException e) {
            // Log and return
            writeToUiAppend(textView, "ERROR: Tag lost exception");
            return null;
        } catch (IOException e) {
            writeToUiAppend(textView, "ERROR: IOEexception: " + e);
            e.printStackTrace();
            return null;
        }
    }


    /**
     * service methods
     */

    /**
     * The input value is SHA-256 hashed and the first 8 bytes are returned
     * @param uid
     * @return
     */
    public byte[] getUidHashShort (byte[] uid) {
        MessageDigest digest = null;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            return null;
        }
        return Arrays.copyOf(digest.digest(uid), 4);
    }

    /*
    private void writeToUiAppendOld(TextView textView, String message) {
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
     */

    private void writeToUiAppend(TextView textView, String message) {
        String oldString = textView.getText().toString();
        if (TextUtils.isEmpty(oldString)) {
            textView.setText(message);
        } else {
            String newString = message + "\n" + oldString;
            textView.setText(newString);
            System.out.println(message);
        }
    }
}
