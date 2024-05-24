package com.mag.slam3dvideo.utils

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class OrbSlamSettings {

    @SerialName("File.version")
    var version: String = "1.0"

    @SerialName("Camera.type")
    var cameraType: String = "PinHole"

    @SerialName("Camera1.fx")
    var fx: Double = 1364.21109

    @SerialName("Camera1.fy")
    var fy: Double = 1350.67390

    @SerialName("Camera1.cx")
    var cx: Double = 960.52704

    @SerialName("Camera1.cy")
    var cy: Double = 523.65354

    @SerialName("Camera1.k1")
    var k1: Double = 0.16906

    @SerialName("Camera1.k2")
    var k2: Double = -0.84008

    @SerialName("Camera1.p1")
    var p1: Double = -0.00582

    @SerialName("Camera1.p2")
    var p2: Double = 0.00179

    //# Camera frames per second
    @SerialName("Camera.fps")
    var fps: Int = 30

    @SerialName("Camera.width")
    var width: Int = 1920

    @SerialName("Camera.height")
    var height: Int = 1080

    @SerialName("Camera.newWidth")
    var newWidth: Int = 960

    @SerialName("Camera.newHeight")
    var newHeight: Int = 540

    @SerialName("Camera.imageScale")
    var imageScale: Double = 0.5

    //# Color order of the images (0: BGR 1: RGB. It is ignored if images are grayscale)
    @SerialName("Camera.RGB")
    var RGB: Int = 1

//#--------------------------------------------------------------------------------------------
//# ORB Parameters
//#--------------------------------------------------------------------------------------------

    //# ORB Extractor: Number of features per image
    @SerialName("ORBextractor.nFeatures")
    var nFeatures: Int = 2000 //1000

    //# ORB Extractor: Scale factor between levels in the scale pyramid
    @SerialName("ORBextractor.scaleFactor")
    var scaleFactor: Double = 1.2

    //# ORB Extractor: Number of levels in the scale pyramid
    @SerialName("ORBextractor.nLevels")
    var nLevels: Int = 8

    //# ORB Extractor: Fast threshold
//# Image is divided in a grid. At each cell FAST are extracted imposing a minimum response.
//# Firstly we impose iniThFAST. If no corners are detected we impose a lower varue minThFAST
//# You can lower these varues if your images have low contrast
    @SerialName("ORBextractor.iniThFAST")
    var iniThFAST: Int = 20

    @SerialName("ORBextractor.minThFAST")
    var minThFAST: Int = 7

    //#--------------------------------------------------------------------------------------------
//# Viewer Parameters
//#---------------------------------------------------------------------------------------------
//    Viewer.KeyFrameSize: 0.1
    @SerialName("Viewer.KeyFrameSize")
    var keyFrameSize: Double = 0.1

    @SerialName("Viewer.KeyFrameLineWidth")
    var keyFrameLineWidth: Double = 1.0

    @SerialName("Viewer.GraphLineWidth")
    var graphLineWidth: Double = 1.0

    @SerialName("Viewer.PointSize")
    var pointSize: Double = 2.0

    @SerialName("Viewer.CameraSize")
    var cameraSize: Double = 0.15

    @SerialName("Viewer.CameraLineWidth")
    var cameraLineWidth: Double = 2.0

    @SerialName("Viewer.ViewpointX")
    var viewpointX: Double = 0.0

    @SerialName("Viewer.ViewpointY")
    var viewpointY: Double = -10.0

    @SerialName("Viewer.ViewpointZ")
    var viewpointZ: Double = -0.1

    @SerialName("Viewer.ViewpointF")
    var viewpointF: Double = 2000.0
}