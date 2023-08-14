# Mifare DESFire EV3 SUN Feature

# Activation of Secure Dynamic Messaging (SDM) for Secure Unique NFC (SUN) feature

The SDM/SUN feature is available on Mifare DESFire EV3 card types only. It is very useful if your business case is to work
with a "standard" reader infrastructure that are smartphones (Android or Apple) without usage of a dedicated app installed
on the phone.

## What is a SDM/SUN message ?

As you can format (parts of) a Mifare DESFire tag in **NDEF mode** the tag will respond to an attached reader with the data that is
stored in the NDEF data file. There are several NDEF message types available, but the SDM/SUN feature uses the **URL record** type
where an URL is stored that points to a backend server. When the tag is tapped to a smartphone an (installed) application will
open that is capable of working with URL data, usually your browser will will open and tries to connect to the URL provided by
the tag.

The backend server can verify the data provided by the link and act on the data, e.g. open a door or buy a transport ticket.

## How does SDM work ?

Below you find a sample **URL** that points to a (backend) server:

https://sdm.nfcdeveloper.com/

When using this link you get some information about a "Secure Dynamic Messaging Backend Server Example" that can be used for
NTAG 424 DNA tags but for DESFire EV3 as well but, beware, when you carefully read the examples you may find that the full
URL looks like

https://sdm.nfcdeveloper.com/tag?picc_data=EF963FF7828658A599F3041510671E88&cmac=94EED9EE65337086

so the "real" endpoint ("**Base URL**") is something like

https://sdm.nfcdeveloper.com/tag

followed by data fields like "uid", "ctr", "picc_data" or "cmac".

That brings us to the **Template URL** that could look like this URL:

https://sdm.nfcdeveloper.com/tag?picc_data=00000000000000000000000000000000&cmac=0000000000000000

If you use the template URL on the backend server you will receive a "400 Bad Request: Invalid message (most probably wrong signature)" error.
That is due to the fact that this template URL does not contain any real data - they would be in the "00"er fields that act as a
placeholder for real data.

If you write the URL using a NDEF message to the NDEF file a tapped device will open the browser, connects to the backend server and -
nothing will happen as the SDM feature is not enabled so far.

## How to enable SDM on a Mifare DESFire EV3 tag ?

To make it very short, you tell the tag that from now on the SDM feature is enabled and the tag should provide data like the UID and the
reader counter as part of the link. When tapping the tag to a reader device the tag will copy the requested "real data" into the
placeholder positions so that the URL will look like this:

https://sdm.nfcdeveloper.com/tagpt?uid=041E3C8A2D6B80&ctr=000006&cmac=4B00064004B0B3D3

Using this URL the backend server will respond like this:

```plaintext
Cryptographic signature validated.
Encryption mode: AES
NFC TAG UID: 041e3c8a2d6b80
Read counter: 6
```

If the door opener acts on a "white list with approved UID's" the door could get open now.

This is an bad example because we are sending confidential data like the card's UID over an insecure infrastructure and we
should change the "Plain" data transmission to an "Encrypted" one.

## How to change the transmission from "Plain" to "Encrypted" mode

The advantage of a Plain transmission is that we do not need anything special like "encryption keys" or "algorithms"
to run the  transmission but the disadvantage is: everyone can read out the (confidential) data. For that reason
the DESFire EV3 tag supports the "Encrypted" mode that needs an additional parameter. As "Encrypted" data needs to get decrypted
both parties need to agree on an **encryption key** that is used for encryption and decryption ("symmetric encryption").

On **creation of an application** on a DESFire tag you setup up to 14 keys that can act for several purposes. When **creating
a file** you define which key is used for a dedicated purpose (in most times it is an access right like "read" or "write"). For
Encrypted SDM features you define a well one of those keys as encryption keys and the backend server needs to know this specific
key for decryption.

## What data is provided in the SUN message ?

There are 4 data fields available within a SUN message:

1) UID: This is the card's UID (a unique number). This element is 7 bytes long but during mirroring it is encoded as hex encoded string,  
   so it is 14 characters long
2) Read Counter: every read access on the file increases the read counter (starting with 0 after file creation). The read counter is a
   3 bytes long array (the value is LSB encoded) but on mirroring it is encoded as hex encoded string, so it is 6 characters long
3) Encrypted File Data (EncFileData): During SDM setup a template URL is written to the NDEF file that has placeholders for each data element. The
   placeholder for the EncFileData element can contain confidential data that needs to provided to the background server (for an example see below)
   that is static to this card. On mirroring the plain data within this placeholder gets encrypted and the encrypted file data will overwrite the
   plain data. The EncFileData needs to be a multiple of 32 but only the first 16 bytes are getting encrypted and provided as a hex encoded  
   string, so the EncFileData is 32 characters long (when the placeholder is 32 characters long).
4) CMAC: the data provided by the tag is secured by a digital signature, so the background server is been able to validate the message against
   tampering.

Instead of UID and Read Counter in plain transmission you can choose to use Encrypted PICC data instead. Using this feature the UID AND Read Counter
are part of the sun message but as an encrypted data field.

## What is a use case for using Encrypted File Data ?

Think of an application where the SUN message acts as a door opener to an Entertainment business. All allowed cards are on a "whitelist" that
holds the UID of the tag - if the card's is found on the whitelist the door will open. But how do permit the access for age reasons ? Of course,
you can use different whitelists but your members are getting older every day and you would be forced to maintain your whitelist every day.

A more easy way is to store the birthday on member's card within the EncFileData placeholder space (e.g. "2001-01-17"). This value gets encrypted
on mirroring while reading the NDEF message at the door's reader device. The card presents the birthday in encrypted form that changes on every read
so there is no chance for a replay attack or other tampering.



## How to decrypt the data ?

Testdata: https://sdm.nfcdeveloper.com/tag?picc_data=7611174D92F390458FF7E15ACFD2579F&enc=F9FB7442DB2E0BE631CD4E3BCF74276E&cmac=69784EF122D0CB5F

```plaintext
Cryptographic signature validated.
Encryption mode: AES
PICC Data Tag: c7
NFC TAG UID: 04514032501490
Read counter: 82
File data (hex): 30313032303330343035303630373038
File data (UTF-8): 0102030405060708
```


Test in PHP:  https://replit.com/@javacrypto/PhpDecryptSunMessage#index.php

## What are the 'Offsets' in the documents ?

We are going to work with the most used template URL to show how the Offset concept is working.

This is the template URL I'm using:

https://sdm.nfcdeveloper.com/tag?picc_data=00000000000000000000000000000000&cmac=0000000000000000

but it is not stored in this form because it is within a NDEF Record with type URL, so there are
some header bytes for the NDEF encapsulating. The URL is written to the tag this  way:

sdm.nfcdeveloper.com/tag?picc_data=00000000000000000000000000000000&cmac=0000000000000000

This important because I'm trying to find the  offset positions by searching within a string.

```plaintext

         10        20        30        40        50        60        70        80        90        100
1234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890
sdm.nfcdeveloper.com/tag?picc_data=00000000000000000000000000000000&cmac=0000000000000000
                                   | EncPiccDataOffset                   | CMACOffset
Length of placeholder              |      32 chars = 16 bytes     |      | 16 ch./8 byt.|  


```






# THIS IS THE DESCRIPTION OF ANOTHER APP !!

## Note on this description

This is the description of the project but not of this app - I will edit this at a later point !


This is a Storage Management Application that helps in managing all cartons and goods stored in a Deposit room. 
The app is using NFC tags of type NXP NTAG 21x to easily identify each position by simply reading the tag with 
an NFC reader embedded in most modern Android smartphones.

There are 3 different tag types available for the NTAG 21x family:
- NTAG213 with 144 bytes of freely available user memory, 137 bytes NDEF capacity
- NTAG215 with 504 bytes of freely available user memory, 480 bytes NDEF capacity
- NTAG216 with 888 bytes of freely available user memory, 868 bytes NDEF capacity

The tags do have a build-in NDEF capability so they are read- and writable with common readers. The provide 
a 7-byte serial number UID) that was programmed by the  manufacturer and are immutable. As additional 
feature the tags can **mirror the UID and a reader counter** into user memory so the can get part of an NDEF   
message. The tags, especially the NTAG213, is available for less as a sticker so it can be easily attached 
to the carton.

As this is a complex system some parts of the work are done using the smartphone and other via Webinterface, 
so will need to available space on a webserver you own. To connect the tag with smartphone and webserver  
I'm using the UID as identifier for all datasets. 

The webserver-URL is coded in the NDEF message as a link like the following example:

https://www.example.com/storage/ident&uid=0123456789ABCDEFx112233

The workflow for the management is as follows:

**Preparation before usage*
1 The tags are written for the first usage with an NDEF message that contains the link to a webpage. The mirror 
function is enabled for the UID and the counter, after that the tag get write disabled
2 The tag is identified by the app and an empty webspace file is created (internet connection necessary)
**Usage workflow at storage place**
3 The tag is attached to a carton and read by the app. The user manually adds a carton number (usually something 
written in big letters at the carton), this information is stored in the app internal database
4 The user can make up to 3 photos of the content with the smartphone's camera
5 edit the dataset by manually type in the content (not recommended)
**Usage workflow at the office**
6 Using an internet connection the app is uploading some data to the webspace like cartons content (if collected) 
and the photos
7 Edit the content file for each carton to provide more information about the content using the webspace editor
8 download the content from the webspace to the internal database on the smartphone to have an offline source

Some minor actions can happen: 
- delete an entry because the carton is permanently removed
- mark an entry as absent because the carton is temporary removed
- add/modify/delete some photos

Enhancements:
- encrypt the data on webspace
- use two or more tags to identify the same cartoon (because the tag is attached to a carton side that it not more 
accessible due to storage place)
- add an information where the storage place is in detail (e.g. "row last on left side, 2nd from botton")
- use a multi-user/multi-app system

## Project status: not started yet

Datasheet for NTAG21x: https://www.nxp.com/docs/en/data-sheet/NTAG213_215_216.pdf

Icons: https://www.freeiconspng.com/images/nfc-icon

Nfc Simple PNG Transparent Background: https://www.freeiconspng.com/img/20581

<a href="https://www.freeiconspng.com/img/20581">Nfc Png Simple</a>

Author: Ahkâm, Image License: Personal Use Only

[Icon / Vector editor: https://editor.method.ac/

Minimum SDK is 21 (Android 5)

android:inputType="text|textNoSuggestions"]()