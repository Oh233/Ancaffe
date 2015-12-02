# Ancaffe

An (still under-development) android version of Caffe (https://github.com/BVLC/caffe). Really thanks the work done by sh1r0 of building the Android lib dependency files (https://github.com/sh1r0/caffe-android-lib).

Although sh1r0 already has a nice demo of how caffe works on Android, I build this app is because my work need and also to practise my programming skills. This app is essentially a real-time scene recognition using Caffe on Android device. No network access is needed.

If you want to try this application, first you need fetch the model from my dropbox and then push them into your ```/sdcard/caffe_mobile/``` folder. You can either do this mannully or use the following command.
```
adb shell mkdir -p /sdcard/caffe_mobile/
adb push /PathToDownloadFiles/bvlc_reference_caffenet/ /sdcard/caffe_mobile/bvlc_reference_caffenet/
```

After finishing this, just download all the stuffs from github and open it with android studio then it will work. (Welcome to report bugs). And if you want to customize it, you may refer to sh1r0's tutorial on how to build the .so files, which is the same link given above. 
