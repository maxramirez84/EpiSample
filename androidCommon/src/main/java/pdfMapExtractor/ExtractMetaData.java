package pdfMapExtractor;

// Methods to extract geospatial information from an ArcGIS-exported PDF file.
// Written by Georgia Tech Research Institute
// November 9, 2016

import android.graphics.Bitmap;

import com.tom_roush.pdfbox.cos.COSArray;
import com.tom_roush.pdfbox.cos.COSDictionary;
import com.tom_roush.pdfbox.pdmodel.PDDocument;
import com.tom_roush.pdfbox.pdmodel.PDPage;
import com.tom_roush.pdfbox.pdmodel.PDPageTree;
import com.tom_roush.pdfbox.pdmodel.PDResources;
import com.tom_roush.pdfbox.rendering.PDFRenderer;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

// These are used for manipulating the images...


// This is an example on how to extract metadata from a PDF document.
public final class ExtractMetaData
{
    private ExtractMetaData()
    {
    }

    public static void main(String[] args) throws IOException
    {
        if (args.length != 1)
        {
            usage();
            System.exit(1);
        }
        else
        {
            Bitmap pdfBitmap = null;
            ArrayList<Float> gpsCoords = new ArrayList<Float>();
            pdfBitmap = ParsePDF(args[0], gpsCoords);
        }
    }

    // ParsePDF: Read a PDF file containing geospatial information and do both of the following:
    // 1. Write a PNG file of the map image into the current directory.
    // 2. Return the GPS (Lat/Long) coordinates of the corners of the image as recorded in the PDF.
    //
    // GPS coordinates are reported in the following order:
    // gpsCoords[0] = Latitude of bottom left corner of map
    // gpsCoords[1] = Longitude of bottom left corner of map
    // gpsCoords[2] = Latitude of top left corner of map
    // gpsCoords[3] = Longitude of top left corner of map
    // gpsCoords[4] = Latitude of top right corner of map
    // gpsCoords[5] = Longitude of top right corner of map
    // gpsCoords[6] = Latitude of bottom right corner of map
    // gpsCoords[7] = Longitude of bottom right corner of map
    public static Bitmap ParsePDF(String pdfFilename, ArrayList<Float> gpsCoords) throws IOException {
        PDDocument document = null;
        Bitmap pdfBitmap = null;
        try {
            File f = new File(pdfFilename);
            document = PDDocument.load(f);

            // ------------------------------------------------------------------------------------------
            // Pull out the Geospatial information to use in placing the image. This data comes
            // from Adobe Supplement to the ISO 32000. See Section 8.8 Measurement Properties
            // ------------------------------------------------------------------------------------------

            PDPageTree list = document.getPages();
            int pageIndex = 0;
            // Iterate over each page. We assume there's only one page, but this code will loop over however many there are.
            // One potential problem: if there are multiple pages, this code will only return an image and coordinates for the last page.
            for (PDPage page : list) {
                PDResources pdResources = page.getResources();
                // Get the current page items as a dictionary
                COSDictionary dictionary = page.getCOSObject();
                // Get the viewport dictionary (contains the measurement dictionary)
                COSArray viewport = (COSArray) dictionary.getDictionaryObject("VP");
                //System.out.println("Viewport dictionary: " + viewport.toString());
                COSDictionary dict = (COSDictionary) viewport.getObject(0);

                if (dict != null) {
                    COSDictionary measure = (COSDictionary) dict.getDictionaryObject("Measure");
                    if (measure != null) {
                        // --- Bounds:   (Optional; ExtensionLevel 3) For maps, this
                        // bounding polygon is know as a neatline. These numbers are
                        // expressed relative to a unit square that describes the BBox
                        // associated with a Viewport or form XObject, or the bounds of an
                        // image Xobject. If not present, the default values define a rectangle describing the
                        // full unit square, with values of [0.0 0.0 0.0 1.0 1.0 1.0 1.0 0.0].
                        COSArray bounds = (COSArray) measure.getDictionaryObject("Bounds");
                        System.out.println("Bounds Array: " + bounds);
                        // --- GCS:  (Required; ExtensionLevel 3) A projected or geographic coordinate system dictionary.
                        COSDictionary gcs = (COSDictionary) measure.getDictionaryObject("GCS");
                        System.out.println("Geographic Coordinate System (GCS) Dictionary: " + gcs);

                        // ----------------------------------------------------------------------------
                        // The following don't appear in the ArcGIS output files, so we ignore them
                        // --- DCS: (Optional; ExtensionLevel 3) A projected or geographic coordinate
                        // system to be used for the display of position values, such as
                        // latitude and longitude. Formatting the displayed representation of
                        // these values is controlled by the conforming reader.
                        //COSDictionary dcs = (COSDictionary)measure.getDictionaryObject( "DCS" );
                        //System.out.println("Display Coordinate System (DCS) Dictionary: " + dcs);
                        // --- PDU: (Optional; ExtensionLevel 3) Preferred Display Units. An array of
                        // three names that identify in order a linear display unit, an area
                        // display unit, and an angular display unit.
                        //COSArray pdu = (COSArray)measure.getDictionaryObject( "PDU" );
                        //System.out.println("Preferred Display Units (PDU) Array: " + pdu);
                        // ----------------------------------------------------------------------------


                        // --- GPTS:  (Required; ExtensionLevel 3) An array of numbers taken pairwise,
                        // defining points in geographic space as degrees of latitude and
                        // longitude. These values are based on the geographic coordinate
                        // system described in the GCS dictionary. (Note that any projected
                        // coordinate system includes an underlying geographic coordinate
                        // system.)
                        COSArray gpts = (COSArray) measure.getDictionaryObject("GPTS");
                        if (gpts != null) {
                            float imageCoords[] = gpts.toFloatArray();
                            for (int i = 0; i < gpts.size(); i++) {
                                gpsCoords.add(imageCoords[i]);
                            }
                        }
                        // --- LPTS:  (Optional; ExtensionLevel 3) An array of numbers taken pairwise that
                        // define points in a 2D unit square. The unit square is mapped to the
                        // rectangular bounds of the viewport, image XObject, or forms
                        // XObject that contain the measure dictionary. This array contains
                        // the same number of number pairs as the GPTS array; each number
                        // pair is the unit square object position corresponding to the
                        // geospatial position in the GPTS array.
                        // (This exists in the ArcGIS files but we aren't using it.)
                        //COSArray lpts = (COSArray)measure.getDictionaryObject( "LPTS" );
                        //System.out.println("LPTS Array: " + lpts);
                    }
                }

                if (pdfFilename.indexOf(".") > 0)    // remove any extension since we're going to reuse the name for the output file
                    pdfFilename = pdfFilename.substring(0, pdfFilename.lastIndexOf("."));

                // Write the entire page as an image...
                PDFRenderer pdfRenderer = new PDFRenderer(document);
                pdfBitmap = pdfRenderer.renderImage(0, 1, Bitmap.Config.RGB_565);

            }


        }
        finally {
            if (document != null) {
                document.close();
            }
            else
            {}
        }
        return pdfBitmap;
    }

    private static void usage()
    {
        System.err.println("Usage: java " + ExtractMetaData.class.getName() + " <input-pdf>");
    }
}

