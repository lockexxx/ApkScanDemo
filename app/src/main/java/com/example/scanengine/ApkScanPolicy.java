package com.example.scanengine;

/**
 * @author locke
 */
public class ApkScanPolicy {

    private ApkScanTask.ScanTargetDir[] mScanTargets = {
            new ApkScanTask.ScanTargetDir("/Android/data/com.nhn.android.appstore/temporaris", 2, true),
            new ApkScanTask.ScanTargetDir("/Android/data/com.coolapk.market/download", 0, true),
            new ApkScanTask.ScanTargetDir("/data/com.iconnect.app.pts/apk_download", 2, true),
            new ApkScanTask.ScanTargetDir("/Android/data/com.amazon.venezia/cache", 0, true),
            new ApkScanTask.ScanTargetDir("/muzhiwan/com.muzhiwan.market/gpk", 1, true),
            new ApkScanTask.ScanTargetDir("/sina/weibo/SinaAppMarket/apk", 0, true),
            new ApkScanTask.ScanTargetDir("/91 WireLess/PandaSpace/apps", 2, true),
            new ApkScanTask.ScanTargetDir("/tencent/Tencentnews/market", 2, true),
            new ApkScanTask.ScanTargetDir("/baidu/AppSearch/downloads", 2, true),
            new ApkScanTask.ScanTargetDir("/baidu/SearchBox/downloads", 2, true),
            new ApkScanTask.ScanTargetDir("/AppStoreVn/Download/apps", 2, true),
            new ApkScanTask.ScanTargetDir("/.mingyouGames/moregames", 4, true),
            new ApkScanTask.ScanTargetDir("/Tencent/tassistant/apk", 2, true),
            new ApkScanTask.ScanTargetDir("/htcmarket/app/data/apk", 3, true),
            new ApkScanTask.ScanTargetDir("/tencent/qube/Download", 2, true),
            new ApkScanTask.ScanTargetDir("/System/APK/COMMON/apk", 2, true),
            new ApkScanTask.ScanTargetDir("/Coolpad/coolmart/Apk", 2, true),
            new ApkScanTask.ScanTargetDir("/icm/bazaar/downloads", 2, true),
            new ApkScanTask.ScanTargetDir("/Coolpad/软件商店/apk", 2, true),
            new ApkScanTask.ScanTargetDir("/wandoujia/mario/app", 2, true),
            new ApkScanTask.ScanTargetDir("/Download/.um/apk", 2, true),
            new ApkScanTask.ScanTargetDir("/Android/data", 3, true),
            new ApkScanTask.ScanTargetDir("/wandoujia/app", 0, true),
    };

    public ApkScanTask.ScanTargetDir[] getScanTargetDirs() {
        return mScanTargets;
    }

}
