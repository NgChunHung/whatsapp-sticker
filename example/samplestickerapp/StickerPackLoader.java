/*
 * Copyright (c) WhatsApp Inc. and its affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.example.samplestickerapp;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.StrictMode;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import static com.example.samplestickerapp.StickerContentProvider.ANDROID_APP_DOWNLOAD_LINK_IN_QUERY;
import static com.example.samplestickerapp.StickerContentProvider.ANIMATED_STICKER_PACK;
import static com.example.samplestickerapp.StickerContentProvider.AVOID_CACHE;
import static com.example.samplestickerapp.StickerContentProvider.IMAGE_DATA_VERSION;
import static com.example.samplestickerapp.StickerContentProvider.IOS_APP_DOWNLOAD_LINK_IN_QUERY;
import static com.example.samplestickerapp.StickerContentProvider.LICENSE_AGREENMENT_WEBSITE;
import static com.example.samplestickerapp.StickerContentProvider.PRIVACY_POLICY_WEBSITE;
import static com.example.samplestickerapp.StickerContentProvider.PUBLISHER_EMAIL;
import static com.example.samplestickerapp.StickerContentProvider.PUBLISHER_WEBSITE;
import static com.example.samplestickerapp.StickerContentProvider.STICKER_FILE_EMOJI_IN_QUERY;
import static com.example.samplestickerapp.StickerContentProvider.STICKER_FILE_NAME_IN_QUERY;
import static com.example.samplestickerapp.StickerContentProvider.STICKER_PACK_ICON_IN_QUERY;
import static com.example.samplestickerapp.StickerContentProvider.STICKER_PACK_IDENTIFIER_IN_QUERY;
import static com.example.samplestickerapp.StickerContentProvider.STICKER_PACK_NAME_IN_QUERY;
import static com.example.samplestickerapp.StickerContentProvider.STICKER_PACK_PUBLISHER_IN_QUERY;

class StickerPackLoader {

    /**
     * Get the list of sticker packs for the sticker content provider
     */
    @NonNull
    static ArrayList<StickerPack> fetchStickerPacks(Context context) throws IllegalStateException, MalformedURLException {
       // URL url = new URL("https://www.police.gov.hk/m/ws/sticker_packs.json");
        Uri uri =  Uri.parse( "https://www.police.gov.hk/m/ws/sticker_packs.json" );
        final Cursor cursor = context.getContentResolver().query(StickerContentProvider.AUTHORITY_URI, null, null, null, null);
        Log.d("test17", String.valueOf(StickerContentProvider.AUTHORITY_URI));
        if (cursor == null) {
            throw new IllegalStateException("could not fetch from content provider, " + BuildConfig.CONTENT_PROVIDER_AUTHORITY);
        }
////////////////////////////////////////////

        HashSet<String> identifierSet = new HashSet<>();
        final ArrayList<StickerPack> stickerPackList = fetchFromContentProvider(cursor);
        for (StickerPack stickerPack : stickerPackList) {
            if (identifierSet.contains(stickerPack.identifier)) {
                throw new IllegalStateException("sticker pack identifiers should be unique, there are more than one pack with identifier:" + stickerPack.identifier);
            } else {
                identifierSet.add(stickerPack.identifier);
            }
        }
        if (stickerPackList.isEmpty()) {
            throw new IllegalStateException("There should be at least one sticker pack in the app");
        }


        for (StickerPack stickerPack : stickerPackList) {


            for (int i = 0 ; i < stickerPackList.size() ; i++){
                Log.d("teststicker" , stickerPackList.get(i).toString());

            }

            final List<Sticker> stickers = getStickersForPack(context, stickerPack);

            stickerPack.setStickers(stickers);


                Log.d("teststicker" , String.valueOf(stickers));



            StickerPackValidator.verifyStickerPackValidity(context, stickerPack);
        }
        return stickerPackList;
    }

    @NonNull
    private static List<Sticker> getStickersForPack(Context context, StickerPack stickerPack) throws MalformedURLException {


        final List<Sticker> stickers = fetchFromContentProviderForStickers(stickerPack.identifier, context.getContentResolver());
        for (int i = 0 ; i < stickers.size() ; i++) {
            Log.d("test23", stickers.get(i).toString());
        }


        for (Sticker sticker : stickers) {
            final byte[] bytes;
            try {
                bytes = fetchStickerAsset(stickerPack.identifier, sticker.imageFileName, context.getContentResolver());
                if (bytes.length <= 0) {
                    Log.d("test16", "empty");
                    throw new IllegalStateException("Asset file is empty, pack: " + stickerPack.name + ", sticker: " + sticker.imageFileName);
                }
                sticker.setSize(bytes.length);

            } catch (IOException | IllegalArgumentException e) {
                throw new IllegalStateException("Asset file doesn't exist. pack: " + stickerPack.name + ", sticker: " + sticker.imageFileName, e);
            }
        }
        for (int i = 0 ; i < stickers.size() ; i++) {
            Log.d("test20", stickers.get(i).toString());
        }
        return stickers;
    }




    @NonNull
    private static ArrayList<StickerPack> fetchFromContentProvider(Cursor cursor) {
        ArrayList<StickerPack> stickerPackList = new ArrayList<>();
        cursor.moveToFirst();
        do {
            final String identifier = cursor.getString(cursor.getColumnIndexOrThrow(STICKER_PACK_IDENTIFIER_IN_QUERY));
            final String name = cursor.getString(cursor.getColumnIndexOrThrow(STICKER_PACK_NAME_IN_QUERY));
            final String publisher = cursor.getString(cursor.getColumnIndexOrThrow(STICKER_PACK_PUBLISHER_IN_QUERY));
            final String trayImage = cursor.getString(cursor.getColumnIndexOrThrow(STICKER_PACK_ICON_IN_QUERY));
            final String androidPlayStoreLink = cursor.getString(cursor.getColumnIndexOrThrow(ANDROID_APP_DOWNLOAD_LINK_IN_QUERY));
            final String iosAppLink = cursor.getString(cursor.getColumnIndexOrThrow(IOS_APP_DOWNLOAD_LINK_IN_QUERY));
            final String publisherEmail = cursor.getString(cursor.getColumnIndexOrThrow(PUBLISHER_EMAIL));
            final String publisherWebsite = cursor.getString(cursor.getColumnIndexOrThrow(PUBLISHER_WEBSITE));
            final String privacyPolicyWebsite = cursor.getString(cursor.getColumnIndexOrThrow(PRIVACY_POLICY_WEBSITE));
            final String licenseAgreementWebsite = cursor.getString(cursor.getColumnIndexOrThrow(LICENSE_AGREENMENT_WEBSITE));
            final String imageDataVersion = cursor.getString(cursor.getColumnIndexOrThrow(IMAGE_DATA_VERSION));
            final boolean avoidCache = cursor.getShort(cursor.getColumnIndexOrThrow(AVOID_CACHE)) > 0;
            final boolean animatedStickerPack = cursor.getShort(cursor.getColumnIndexOrThrow(ANIMATED_STICKER_PACK)) > 0;



            final StickerPack stickerPack = new StickerPack(identifier, name, publisher, trayImage, publisherEmail, publisherWebsite, privacyPolicyWebsite, licenseAgreementWebsite, imageDataVersion, avoidCache, animatedStickerPack);
            stickerPack.setAndroidPlayStoreLink(androidPlayStoreLink);
            stickerPack.setIosAppStoreLink(iosAppLink);
            stickerPackList.add(stickerPack);
        } while (cursor.moveToNext());

        return stickerPackList;
    }

    @NonNull
    private static List<Sticker> fetchFromContentProviderForStickers(String identifier, ContentResolver contentResolver) throws MalformedURLException {
        Uri uri = getStickerListUri(identifier);
        Log.d("testuri" , String.valueOf(uri));
       // URL url = new URL("https://www.police.gov.hk/m/ws/sticker_packs.json");
        final String[] projection = {STICKER_FILE_NAME_IN_QUERY, STICKER_FILE_EMOJI_IN_QUERY};

        for (int i = 0 ; i < projection.length ; i++){
            Log.d("test007" , projection[i]);}

        final Cursor cursor = contentResolver.query(uri, projection, null, null, null);
        List<Sticker> stickers = new ArrayList<>();
        if (cursor != null && cursor.getCount() > 0) {
            cursor.moveToFirst();
            do {
                final String name = cursor.getString(cursor.getColumnIndexOrThrow(STICKER_FILE_NAME_IN_QUERY));
                final String emojisConcatenated = cursor.getString(cursor.getColumnIndexOrThrow(STICKER_FILE_EMOJI_IN_QUERY));
                List<String> emojis = new ArrayList<>(StickerPackValidator.EMOJI_MAX_LIMIT);
                if (!TextUtils.isEmpty(emojisConcatenated)) {
                    emojis = Arrays.asList(emojisConcatenated.split(","));
                }
                Log.d("test009" , name);
                stickers.add(new Sticker(name, emojis));


            } while (cursor.moveToNext());
        }
        if (cursor != null) {
            cursor.close();
        }
        return stickers;
    }









    static byte[] fetchStickerAsset(@NonNull final String identifier, @NonNull final String name, ContentResolver contentResolver) throws IOException {
        if (android.os.Build.VERSION.SDK_INT > 9) {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);
        }

        URL url = new URL("https://www.police.gov.hk/m/ws/www/img/"+name);
      Log.d("test15", name);
        InputStream contentsInputStream = url.openStream();
         //   final InputStream inputStream = contentResolver.openInputStream(getStickerAssetUri(identifier, name));
                  final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            if (contentsInputStream == null) {
                throw new IOException("cannot read sticker asset:" + identifier + "/" + name);
            }
            int read;
            byte[] data = new byte[16384];

            while ((read = contentsInputStream.read(data, 0, data.length)) != -1) {
                buffer.write(data, 0, read);
                Log.d("test16", String.valueOf(contentsInputStream.read(data, 0, data.length)));
            }
            return buffer.toByteArray();

    }

    private static Uri getStickerListUri(String identifier) {
        return new Uri.Builder().scheme(ContentResolver.SCHEME_CONTENT).authority(BuildConfig.CONTENT_PROVIDER_AUTHORITY).appendPath(StickerContentProvider.STICKERS).appendPath(identifier).build();

    }

    static Uri getStickerAssetUri(String identifier, String stickerName) {

        Uri uri =  Uri.parse( "https://www.police.gov.hk/m/ws/www/img/"+stickerName );
       // Log.d("final", String.valueOf(uri));
        return uri;

       // return new Uri.Builder().scheme(ContentResolver.SCHEME_CONTENT).authority(BuildConfig.CONTENT_PROVIDER_AUTHORITY).appendPath(StickerContentProvider.STICKERS_ASSET).appendPath(identifier).appendPath(stickerName).build();
    }
}
