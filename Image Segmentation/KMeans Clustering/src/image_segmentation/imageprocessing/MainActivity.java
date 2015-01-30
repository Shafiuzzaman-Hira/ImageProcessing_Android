package image_segmentation.imageprocessing;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;

import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BlurMaskFilter;
import android.graphics.BlurMaskFilter.Blur;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.text.Editable;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

public class MainActivity extends Activity implements OnClickListener {

	private ImageView img;
	private Bitmap bmp;
	private Bitmap operation;
	private Button button;
	private Button button1;
	Bitmap b;
	int k;
	Cluster[] clusters;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		button = (Button) findViewById(R.id.button);
		img = (ImageView) findViewById(R.id.imageView1);
		button.setOnClickListener(this);
		button1 = (Button) findViewById(R.id.button1);
		button1.setOnClickListener(this);

		BitmapDrawable abmp = (BitmapDrawable) img.getDrawable();
		bmp = abmp.getBitmap();
	}

	@Override
	public void onClick(View v) {
		if (v.getId() == R.id.button) {
			// call dialog to picture mode camera / gallery
			Image_Picker_Dialog();
		}

		if (v.getId() == R.id.button1) {
			
			

			AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
 
			// set title
			alertDialogBuilder.setTitle("Input the value of K");
			// Set an EditText view to get user input
						final EditText input = new EditText(this);
						alertDialogBuilder.setView(input);
 
			// set dialog message
			alertDialogBuilder
				.setCancelable(false)
				.setPositiveButton("OK",new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog,int id) {
						k = Integer.parseInt(input.getText().toString());
						applyKmeans();
					}
				  })
				.setNegativeButton("Cancel",new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog,int id) {
						// if this button is clicked, just close
						// the dialog box and do nothing
						dialog.cancel();
					}
				});
 
				// create alert dialog
				AlertDialog alertDialog = alertDialogBuilder.create();
 
				// show it
				alertDialog.show();
			}
	}

	private void applyKmeans() {

		int w = b.getWidth();
		int h = b.getHeight();

		clusters = makeCluster();

		int[] map = new int[w * h];

		Arrays.fill(map, -1);

		boolean isClusterUpadte = true;
		while (isClusterUpadte) {
			isClusterUpadte = false;
			for (int y = 0; y < h; y++) {
				for (int x = 0; x < w; x++) {
					int pixel = b.getPixel(x, y);
					Cluster cluster = findClosestCluster(pixel);
					if (map[w * y + x] != cluster.getId()) {
						if (map[w * y + x] != -1) {
							clusters[map[w * y + x]].removePixel(pixel);
						}

						cluster.addPixel(pixel);

						isClusterUpadte = true;

						map[w * y + x] = cluster.getId();
					}
				}
			}

		}
		Bitmap c = Bitmap.createScaledBitmap(b, 150, 150, true);
		for (int y = 0; y < h; y++) {
			for (int x = 0; x < w; x++) {
				int clusterId = map[w * y + x];
				c.setPixel(x, y, clusters[clusterId].getRGB());
			}
		}
		img.setImageBitmap(c);
	}

	Cluster[] makeCluster() {
		Cluster[] result = new Cluster[k];
		int x = 0;
		int y = 0;
		int dx = b.getWidth() / k;
		int dy = b.getHeight() / k;
		for (int i = 0; i < k; i++) {
			result[i] = new Cluster(i, b.getPixel(x, y));
			x += dx;
			y += dy;
		}
		return result;
	}

	public Cluster findClosestCluster(int rgb) {
		Cluster cluster = null;
		int min = Integer.MAX_VALUE;
		for (int i = 0; i < clusters.length; i++) {
			int distance = clusters[i].distance(rgb);
			if (distance < min) {
				min = distance;
				cluster = clusters[i];
			}
		}
		return cluster;
	}

	private void Image_Picker_Dialog() {

		AlertDialog.Builder myAlertDialog = new AlertDialog.Builder(this);
		myAlertDialog.setTitle("Pictures Option");
		myAlertDialog.setMessage("Select Picture Mode");

		myAlertDialog.setPositiveButton("Gallery",
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface arg0, int arg1) {
						Utility.pictureActionIntent = new Intent(
								Intent.ACTION_GET_CONTENT, null);
						Utility.pictureActionIntent.setType("image/*");
						Utility.pictureActionIntent.putExtra("return-data",
								true);
						startActivityForResult(Utility.pictureActionIntent,
								Utility.GALLERY_PICTURE);
					}
				});

		myAlertDialog.setNegativeButton("Camera",
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface arg0, int arg1) {
						Utility.pictureActionIntent = new Intent(
								android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
						startActivityForResult(Utility.pictureActionIntent,
								Utility.CAMERA_PICTURE);
					}
				});
		myAlertDialog.show();
	}

	// After the selection of image you will return on the main activity with
	// bitmap image
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (requestCode == Utility.GALLERY_PICTURE) {
			// data contains result
			// Do some task
			Image_Selecting_Task(data);
		} else if (requestCode == Utility.CAMERA_PICTURE) {
			// Do some task
			Bitmap photo = (Bitmap) data.getExtras().get("data");
			img.setImageBitmap(photo);
			Image_Selecting_Task(data);
		}
	}

	private void Image_Selecting_Task(Intent data) {
		try {
			Utility.uri = data.getData();
			if (Utility.uri != null) {
				// User had pick an image.
				Cursor cursor = getContentResolver()
						.query(Utility.uri,
								new String[] { android.provider.MediaStore.Images.ImageColumns.DATA },
								null, null, null);
				cursor.moveToFirst();
				// Link to the image
				final String imageFilePath = cursor.getString(0);

				// Assign string path to File
				Utility.Default_DIR = new File(imageFilePath);

				// Create new dir MY_IMAGES_DIR if not created and copy image
				// into that dir and store that image path in valid_photo
				Utility.Create_MY_IMAGES_DIR();

				// Copy your image
				Utility.copyFile(Utility.Default_DIR, Utility.MY_IMG_DIR);

				// Get new image path and decode it
				b = Utility.decodeFile(Utility.Paste_Target_Location);

				// use new copied path and use anywhere
				String valid_photo = Utility.Paste_Target_Location.toString();
				b = Bitmap.createScaledBitmap(b, 150, 150, true);

				// set your selected image in image view
				img.setImageBitmap(b);
				cursor.close();

			} else {
				Toast toast = Toast.makeText(this,
						"Sorry!!! You haven't selecet any image.",
						Toast.LENGTH_LONG);
				toast.show();
			}
		} catch (Exception e) {
			// you get this when you will not select any single image
			Log.e("onActivityResult", "" + e);

		}

	}



	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

}
