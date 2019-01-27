# MSP-Emotion-API
The android application code used in the MSP Blog. This android application uses Microsofts Emotion API to get the emotions of images selected by the user from their gallery.

 private void setWallpaper(int x){
        Bitmap bitmap;
        if (x == 1) {
            bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.image1);
        }
        else{
            bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.image2);
        }
        WallpaperManager manager = WallpaperManager.getInstance(getApplicationContext());

        try{
            manager.setBitmap(bitmap);
            Toast.makeText(this, "Wallpaper set!", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            Toast.makeText(this, "Error!", Toast.LENGTH_SHORT).show();
        }
    }
