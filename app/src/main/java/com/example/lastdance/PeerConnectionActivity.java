package com.example.lastdance;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.Camera1Enumerator;
import org.webrtc.Camera2Enumerator;
import org.webrtc.CameraVideoCapturer;
import org.webrtc.DataChannel;
import org.webrtc.EglBase;
import org.webrtc.EglRenderer;
import org.webrtc.IceCandidate;
import org.webrtc.MediaConstraints;
import org.webrtc.MediaStream;
import org.webrtc.MediaStreamTrack;
import org.webrtc.PeerConnection;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.RendererCommon;
import org.webrtc.RtpReceiver;
import org.webrtc.RtpTransceiver;
import org.webrtc.ScreenCapturerAndroid;
import org.webrtc.SdpObserver;
import org.webrtc.SessionDescription;
import org.webrtc.SurfaceTextureHelper;
import org.webrtc.SurfaceViewRenderer;
import org.webrtc.VideoCapturer;
import org.webrtc.VideoSource;
import org.webrtc.VideoTrack;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;

public class PeerConnectionActivity extends AppCompatActivity {


    PeerConnection PC;

    PeerConnection.RTCConfiguration rtcConfig;
    List<PeerConnection.IceServer> iceServers;

    Button 시작,종료;
    Application myApp;
    PeerConnectionFactory connectionFactory;
    MediaConstraints mediaConstraints;

    PeerConnection.Observer PCObserver;
    SdpObserver sdpObserver;

    VideoCapturer videoCapturer;

    EglBase rootEglBase;
    EglBase.Context eglBaseContext;
    SurfaceTextureHelper surfaceTextureHelper;

    final String VIDEO_TRACK_ID = "ARDAMSv0";

    VideoTrack localTrack;
    VideoTrack remoteTrack;

    SurfaceViewRenderer localView,remoteView;

    Socket socket;

    Emitter.Listener onConnect,onMessage;


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == 1) {
            if (grantResults.length > 0 &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED &&
                    grantResults[1] == PackageManager.PERMISSION_GRANTED) {

                // Permissions granted, continue with initializing video/audio tracks
                Toast.makeText(this, "Permissions granted", Toast.LENGTH_SHORT).show();
            } else {
                // Permissions denied
                Toast.makeText(this, "Permissions denied. The app may not work correctly.", Toast.LENGTH_LONG).show();
            }
        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_peer_connection);



        // Check and request camera and microphone permissions
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, android.Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {

            // Request the camera and audio permissions
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.CAMERA, android.Manifest.permission.RECORD_AUDIO},
                    1);
        }

        myApp = (Application) getApplicationContext();
        // 예제 코드
        PeerConnectionFactory.initialize(
                PeerConnectionFactory.InitializationOptions.builder(this)
                        .setEnableInternalTracer(true)
                        .createInitializationOptions()
        );
        connectionFactory = PeerConnectionFactory.builder().createPeerConnectionFactory();
        //connectionFactory = myApp.getPeerConnectionFactory();

        시작 = findViewById(R.id.button3);
        종료 = findViewById(R.id.button5);
        localView = findViewById(R.id.localview);
        remoteView = findViewById(R.id.remoteview);

        SurfaceViewRendererInit(localView);
        SurfaceViewRendererInit(remoteView);

        iceServers = new ArrayList<>();
        PeerConnection.IceServer stunserver =
                PeerConnection.IceServer.builder("stun:stun.l.google.com:19302")
                        .createIceServer();
        iceServers.add(stunserver);

        rtcConfig = new PeerConnection.RTCConfiguration(iceServers);
        rootEglBase = EglBase.create();
        eglBaseContext= rootEglBase.getEglBaseContext();
        surfaceTextureHelper = SurfaceTextureHelper.create(Thread.currentThread().getName(), eglBaseContext);


        mediaConstraints = new MediaConstraints();
        mediaConstraints.mandatory.add(
                new MediaConstraints.KeyValuePair("OfferToReceiveAudio",  Boolean.toString(true)));
        mediaConstraints.mandatory.add(new MediaConstraints.KeyValuePair(
                "OfferToReceiveVideo", Boolean.toString(true)));//isVideoCallEnabled()

        PCObserver = new PeerConnection.Observer() {
            @Override
            public void onSignalingChange(PeerConnection.SignalingState signalingState) {

            }

            @Override
            public void onIceConnectionChange(PeerConnection.IceConnectionState iceConnectionState) {

            }

            @Override
            public void onIceConnectionReceivingChange(boolean b) {

            }

            @Override
            public void onIceGatheringChange(PeerConnection.IceGatheringState iceGatheringState) {

            }

            @Override
            public void onIceCandidate(IceCandidate iceCandidate) {
// const message = {
//                        type: 'candidate',
//                        candidate: null,
//    };
//                if (e.candidate) {
//                    message.candidate = e.candidate.candidate;
//                    message.sdpMid = e.candidate.sdpMid;
//                    message.sdpMLineIndex = e.candidate.sdpMLineIndex;
//                }
//                signaling.postMessage(message)
                JSONObject message = new JSONObject();
                try {
                    message.put("type","candidate");
                    message.put("candidate",toJsonCandidate(iceCandidate));
                } catch (JSONException e) {
                    e.printStackTrace();
                    return;
                }


                socket.emit("message",message);
            }

            @Override
            public void onTrack(RtpTransceiver transceiver) {
                PeerConnection.Observer.super.onTrack(transceiver);
                PeerConnection.Observer.super.onTrack(transceiver);
                PeerConnection.Observer.super.onTrack(transceiver);
                Log.i("onTrack","");
                PeerConnection.Observer.super.onTrack(transceiver);

                RtpReceiver receiver = transceiver.getReceiver();

                if(receiver.track().kind().equals(MediaStreamTrack.VIDEO_TRACK_KIND)){
                    VideoTrack videoTrack = (VideoTrack) receiver.track();
                    Log.i("onTrack","videoTrack");
                    videoTrack.addSink(remoteView);
                }
            }

            @Override
            public void onConnectionChange(PeerConnection.PeerConnectionState newState) {

                PeerConnection.Observer.super.onConnectionChange(newState);

                Log.i("onConnectionChange",newState.toString());

            }

            @Override
            public void onIceCandidatesRemoved(IceCandidate[] iceCandidates) {

            }

            @Override
            public void onAddStream(MediaStream mediaStream) {

            }

            @Override
            public void onRemoveStream(MediaStream mediaStream) {

            }

            @Override
            public void onDataChannel(DataChannel dataChannel) {

            }

            @Override
            public void onRenegotiationNeeded() {

            }
        };
        onConnect = new Emitter.Listener() {
            @Override
            public void call(Object... args) {

                Log.i("연결 성공","connect");

            }
        };
        onMessage = new Emitter.Listener() {
            @Override
            public void call(Object... args) {


                JSONObject message = (JSONObject) args[0];
                String type;
                try {
                    type = message.getString("type");
                } catch (JSONException e) {
                    e.printStackTrace();
                    return;
                }

                switch (type){
                    case "ready":
                        if(PC != null){
                            Log.i("ready : ","already in call, ignoring");
                            break;
                        }
                        makeCall();
                        break;
                    case "offer" :

                        SessionDescription offer;
                        try {

                            String sdp_description = message.getString("sdp");
                            offer = new SessionDescription(SessionDescription.Type.OFFER,sdp_description);
                            handleOffer(offer);

                        } catch (JSONException e) {
                            e.printStackTrace();
                            return;
                        }

                        break;
                    case "answer" :
                        SessionDescription answer;
                        try {

                            String sdp_description = message.getString("sdp");
                            answer = new SessionDescription(SessionDescription.Type.ANSWER,sdp_description);

                        } catch (JSONException e) {
                            e.printStackTrace();
                            return;
                        }
                        handleAnswer(answer);
                        break;
                    case "candidate" :
                        IceCandidate candidate;
                        try {
                            JSONObject msg = (JSONObject) message.get("candidate");
                            candidate = new IceCandidate(  msg.getString("sdpMid"),msg.getInt("sdpMLineIndex"), msg.getString("candidate"));

                        } catch (JSONException e) {
                            e.printStackTrace();
                            return;
                        }
                        handleCandidate(candidate);
                        break;
                    case "bye" :
                        if(PC != null)
                            hangup();
                        break;
                }

            }
        };


        시작.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                // 비디오 트랙 가져오기
                localTrack = getLocalVideo(true);

                Log.i("localTrack","⚠️⚠️⚠️⚠ localTrack finish ⚠️⚠️⚠️⚠️:");


                localTrack.addSink(localView);


                Log.i("addSink","⚠️⚠️⚠️⚠ addSink finish ⚠️⚠️⚠️⚠️:");

                //signaling.postMessage({type: 'ready'});

                JSONObject message = new JSONObject();


                Log.i("JSONObject finish","⚠️⚠️⚠️⚠ JSONObject finish ⚠️⚠️⚠️⚠️:");

                try {
                    message.put("type","ready");
                } catch (JSONException e) {
                    e.printStackTrace();
                }



                socket.emit("message",message);
                Log.i("Socket emit finish","⚠️⚠️⚠️⚠ Socket emit finish ⚠️⚠️⚠️⚠️:");
            }
        });



        종료.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                hangup();
                JSONObject message = new JSONObject();
                try {
                    message.put("type","bye");
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                socket.emit("message",message);

            }
        });

        socket = IO.socket(URI.create("https://webrtc.github.io/samples/src/content/peerconnection/channel/")); // 서버 url 여기 기록! 🥸

        Log.i("Socket emit finish","⚠️⚠️⚠️⚠ Socket emit finish ⚠️⚠️⚠️⚠️:");


        socket.on(Socket.EVENT_CONNECT, onConnect);

        socket.on("onmessage",onMessage);
        socket.connect();

    }


    public void makeCall(){
        createPeerConnection();

        PC.createOffer(new SdpObserver() {
            @Override
            public void onCreateSuccess(SessionDescription sessionDescription) {
                JSONObject message = new JSONObject();
                try {
                    message.put("type","offer");
                    message.put("sdp",sessionDescription.description);

                } catch (JSONException e) {
                    e.printStackTrace();
                    return;
                }
                socket.emit("message",message);

                PC.setLocalDescription(new SdpObserver() {
                    @Override
                    public void onCreateSuccess(SessionDescription sessionDescription) {

                    }

                    @Override
                    public void onSetSuccess() {

                    }

                    @Override
                    public void onCreateFailure(String s) {

                    }

                    @Override
                    public void onSetFailure(String s) {

                    }
                }, sessionDescription);
            }

            @Override
            public void onSetSuccess() {

            }

            @Override
            public void onCreateFailure(String s) {

            }

            @Override
            public void onSetFailure(String s) {

            }
        },mediaConstraints);


    }

    public void createPeerConnection(){

        PC = connectionFactory.createPeerConnection(rtcConfig,PCObserver);
        assert PC != null;
        PC.addTrack(localTrack);

    }

    // icecandidate를 json형식으로 변환한다.
    // Converts a Java candidate to a JSONObject.
    private JSONObject toJsonCandidate(final IceCandidate candidate) {
        JSONObject json = new JSONObject();
        try {
            json.put("sdpMLineIndex", candidate.sdpMLineIndex);
            json.put("sdpMid", candidate.sdpMid);
            json.put("candidate", candidate.sdp);
        } catch (JSONException e) {
            e.printStackTrace();
        }
//        jsonPut(json, "id", candidate.sdpMid);
//        jsonPut(json, "candidate", candidate.sdp);
        return json;
    }

    /**내 기기의(카메라 소스로 얻은) video track을 반환한다.
     *
     *@paramstatus
     *@return:내 기기의(카메라 소스로 얻은) video track
     */
    public VideoTrack getLocalVideo(boolean status){

        // localVideo 변수 선언
        VideoTrack localVideo;

        // videoCapturer : 비디오 소스에서 비디오 프레임을 캡처하고 VideoSource 객체에 전달하는 데 사용됩니다.
        videoCapturer = createCameraCapturer(status);

        if (videoCapturer == null) {
            Log.e("getLocalVideo", "videoCapturer is null");
            return null; // or handle this case appropriately
        }


        // createCameraCapturer {videoCapturer} 출력
        Log.w("createCameraCapturer",videoCapturer.toString());

        // VideoSource 객체를 생성합니다. 이 VideoSource 객체는 로컬 미디어 스트림에서 비디오 소스로 사용됩니다.
        // 1번째 매개변수는 비디오 소스가 카메라 스트림(실시간 비디오)을 사용하는지 또는 비디오 파일 스트림(미리 녹화된 비디오)을 사용하는지를 나타냅니다.
        // videoCapturer.isScreencast() :비디오 캡처기가 화면 녹화 모드에서 동작하는 경우, 즉 사용자가 화면 공유를 수행하는 경우에 true 값을 반환합니다. 반면에 일반적인 카메라 캡처기의 경우, false 값을 반환합니다.
        // 즉, VideoCapturer 객체가 현재 어떤 모드로 동작하는지를 나타내는
        VideoSource videoSource = connectionFactory.createVideoSource(videoCapturer.isScreencast());


        //  비디오 캡처기를 초기화하고 비디오 프레임을 캡처하기 시작하는 데 사용
        // 비디오 캡처기의 콜백 함수를 설정하고, 캡처할 비디오 해상도, 비율 및 프레임 속도 등의 속성을 설정
        //  videoSource.getCapturerObserver() : 비디오 프레임을 전달할 CapturerObserver 객체
        videoCapturer.initialize(surfaceTextureHelper, getApplicationContext(), videoSource.getCapturerObserver());
        Log.i("videoSource.getObserver",videoSource.getCapturerObserver().toString());

        // 비디오 캡쳐 : getUserMedia 로 스트림 받아오기 시작?
        // 비디오 캡처를 시작하고, 캡처된 비디오 프레임을 VideoSink로 전달하기 위해 호출
        // 캡처할 비디오 프레임의 너비, 높이, 및 프레임 속도
        // 240, 320,30
        videoCapturer.startCapture(240, 320, 30);

        //        이 메서드는 로컬 비디오 트랙을 생성하는 데 사용됩니다.
        //  VideoSource 객체와 연결된 VideoTrack을 만듭니다.
        // VIDEO_TRACK_ID은 비디오 트랙 고유 식별자로 사용.
        localVideo = connectionFactory.createVideoTrack(VIDEO_TRACK_ID, videoSource);

        // 내 기기의 (카메라 소스로 얻은) video track
        return localVideo;
        // getLocalVideo 함수의 끝.

    }

    /**
     *
     *@paramisFront
     *@return:생성한videoCapturer반환,생성 실패시null반환.
     */
    private VideoCapturer createCameraCapturer(boolean isFront) {



        // Camera1Enumerator : Android 디바이스의 카메라 목록을 가져오고 선택한 카메라를 열기 위한 클래스
        // 매개변수 : true를 전달하면 전면 카메라만 사용하며, false를 전달하거나 이 매개변수를 생략하면 전면 카메라와 후면 카메라 모두 사용
        //Camera1Enumerator enumerator = new Camera1Enumerator(false);
        Camera2Enumerator enumerator = new Camera2Enumerator(getApplicationContext());

        // Android 디바이스에서 사용 가능한 카메라 디바이스를 열거할 수 있습니다.
        // 카메라의 ID와 이름을 갖는 CameraEnumerationAndroid.CaptureDeviceInfo 객체의 목록을 반환합니다.
        final String[] deviceNames = enumerator.getDeviceNames();


        // First, try to find front facing camera
        // deviceNames 요소의수만큼 반복
        for (String deviceName : deviceNames) {

            Log.i("deviceName : ",deviceName);

            // A ? B : C => 조건 연산자, A가 참이면 B를 반환, 거짓이면 C를 반환
            // Camera1Enumerator.isFrontFacing() : 전면카메라면 true, 후면 카메라라면 false를 반환
            // Camera1Enumerator.isBackFacing() : 후면카메라면 true, 전면 카메라라면 false를 반환
            if (isFront ? enumerator.isFrontFacing(deviceName) : enumerator.isBackFacing(deviceName)) {

                // 매개변수로는 CameraEnumerationAndroid.CaptureDeviceInfo 객체와 CapturerObserver 객체, CameraEventsHandler 객체를 받습니다.
                // deviceName 카메라로 VideoCapturer 객체 생성
                //  선택한 카메라를 열기 위한 메소드
                VideoCapturer videoCapturer = enumerator.createCapturer(deviceName, new CameraVideoCapturer.CameraEventsHandler() {
                    // CameraVideoCapturer에서 발생하는 각 이벤트를 처리하는 기본 구현을 제공하며,
                    // 이벤트를 수신하려면 CameraVideoCapturer.setCameraEventsHandler() 메서드를 사용하여 CameraEventsHandler 객체를 등록해야 합니다.

                    // 오버라이딩, 함수 재정의
                    @Override
                    // 카메라에서 오류가 발생할 때 호출됩니다. 오류 메시지를 매개변수로 받습니다.
                    public void onCameraError(String s) {
                        // onCameraError 메세지 출력.
                        Log.w("onCameraError",s);
                        // onCameraError 함수의 끝.
                    }

                    // 오버라이딩, 함수 재정의
                    @Override
                    // 카메라 연결이 끊어졌을 때 호출됩니다.
                    public void onCameraDisconnected() {
                        // onCameraDisconnected 메세지 출력.
                        Log.w("onCameraDisconnected","");
                        // onCameraDisconnected 함수의 끝.
                    }

                    // 오버라이딩, 함수 재정의
                    @Override
                    // 카메라가 정지되거나 동결될 때 호출됩니다. 오류 메시지를 매개변수로 받습니다.
                    public void onCameraFreezed(String s) {
                        // onCameraFreezed 메세지 출력.
                        Log.w("onCameraFreezed",s);
                        // onCameraFreezed 함수의 끝.
                    }

                    // 오버라이딩, 함수 재정의
                    @Override
                    // 카메라를 열고 있는 동안 호출됩니다. 열려고 하는 카메라의 이름을 매개변수로 받습니다.
                    public void onCameraOpening(String s) {
                        // onCameraOpening 메세지 출력.
                        Log.w("onCameraOpening",s);
                        // onCameraOpening 함수의 끝.
                    }

                    // 오버라이딩, 함수 재정의
                    @Override
                    // 첫 번째 비디오 프레임이 사용 가능할 때 호출됩니다.
                    public void onFirstFrameAvailable() {
                        // onFirstFrameAvailable 메세지 출력.
                        Log.w("onFirstFrameAvailable","");
                        // onFirstFrameAvailable 함수의 끝.
                    }

                    // 오버라이딩, 함수 재정의
                    @Override
                    // 카메라가 닫혔을 때 호출됩니다.
                    public void onCameraClosed() {
                        // onCameraClosed 메세지 출력.
                        Log.w("onCameraClosed","");
                        // onCameraClosed 함수의 끝.
                    }
                    // createCapturer () 끝.
                });

                // 생성성공시 {} 안의 코드 실행.
                if (videoCapturer != null) { // 이거 문제!
                    // 생성한 videoCapturer 반환
                    return videoCapturer;
                    // 생성성공시 실행할 코드 끝.
                }

                // isFront가 true일 때 전면카메라, false일 때 후면 카메라면 실행할 코드 끝.
            }
            /// 반복하는 코드의 끝.
        }

        // null 반환
        Log.e("createCameraCapturer", "No suitable camera found!");

        return null;

        /// createCameraCapturer 함수 끝.
    }

    void SurfaceViewRendererInit(SurfaceViewRenderer view){

        view.setMirror(false);

//        rootEglBase = EglBase.create();
//        eglBaseContext = rootEglBase.getEglBaseContext();
//        Log.i("eglBaseContext : ",eglBaseContext.toString());

//        view.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_BALANCED);
        view.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FIT);
        view.removeFrameListener(new EglRenderer.FrameListener() {
            @Override
            public void onFrame(Bitmap bitmap) {
                Log.i("removeFrameListener :","");
            }
        });
        view.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
            @Override
            public void onViewAttachedToWindow(View v) {
                Log.i("onViewAttached","ToWindow :");

            }

            @Override
            public void onViewDetachedFromWindow(View v) {
                Log.i("onViewDetached","FromWindow :");

            }
        });
        view.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {

            }
        });
        view.init(eglBaseContext,  new RendererCommon.RendererEvents() {
            //            첫 번째 프레임이 렌더링되면 콜백이 실행됩니다.
            @Override
            public void onFirstFrameRendered() {
                Log.i("RendererEvents","onFirstFrameRendered");
//                box.setVisiProfile(false);

            }
            //            렌더링된 프레임 해상도 또는 회전이 변경되면 콜백이 실행됩니다.
            @Override
            public void onFrameResolutionChanged(int i, int i1, int i2) {
                Log.i("RendererEvents","onFrameResolutionChanged");


            }

        });

    }

    public void hangup(){
        // 종료
        PC.close();
        PC = null;

    }
    public void handleOffer(SessionDescription offer){
//        if (pc) {
//            console.error('existing peerconnection');
//            return;
//        }
        if (PC != null) {
            Log.i("","existing peerconnection");
            return;
        }

//        await createPeerConnection();
        createPeerConnection();
//     0   await pc.setRemoteDescription(offer);
//
        Log.i("handleOffer","");
        PC.setRemoteDescription(new SdpObserver() {
            @Override
            public void onCreateSuccess(SessionDescription sessionDescription) {

            }

            @Override
            public void onSetSuccess() {
                Log.i("handleOffer","onSetSuccess");

                PC.createAnswer(new SdpObserver() {
                    @Override
                    public void onCreateSuccess(SessionDescription sessionDescription) {
                        Log.i("createAnswer","onCreateSuccess");

                        JSONObject message = new JSONObject();
                        try {
                            message.put("type","answer");
                            message.put("sdp",sessionDescription.description);
                        } catch (JSONException e) {
                            e.printStackTrace();
                            return;
                        }
                        socket.emit("message",message);

                        PC.setLocalDescription(new SdpObserver() {
                            @Override
                            public void onCreateSuccess(SessionDescription sessionDescription) {

                            }

                            @Override
                            public void onSetSuccess() {

                            }

                            @Override
                            public void onCreateFailure(String s) {

                            }

                            @Override
                            public void onSetFailure(String s) {

                            }
                        },sessionDescription);
                    }

                    @Override
                    public void onSetSuccess() {

                    }

                    @Override
                    public void onCreateFailure(String s) {

                    }

                    @Override
                    public void onSetFailure(String s) {

                    }
                },mediaConstraints);


            }

            @Override
            public void onCreateFailure(String s) {

            }

            @Override
            public void onSetFailure(String s) {

                Log.i("handleoffer","onSetFailure");
                Log.i("handleoffer",s);

            }
        },offer);

//  const answer = await pc.createAnswer();
//        signaling.postMessage({type: 'answer', sdp: answer.sdp});
//        await pc.setLocalDescription(answer);
    }
//
//    async function handleAnswer(answer) {
//        if (!pc) {
//            console.error('no peerconnection');
//            return;
//        }
//        await pc.setRemoteDescription(answer);
//    }

    public void handleAnswer(SessionDescription answer){
        //        if (!pc) {
//            console.error('no peerconnection');
//            return;
//        }
        if(PC == null){
            Log.i("Error","no peerconnection");
            return;
        }

        PC.setRemoteDescription(new SdpObserver() {
            @Override
            public void onCreateSuccess(SessionDescription sessionDescription) {

            }

            @Override
            public void onSetSuccess() {

            }

            @Override
            public void onCreateFailure(String s) {

            }

            @Override
            public void onSetFailure(String s) {

            }
        },answer);
    }

    public void handleCandidate(IceCandidate candidate){
//        if (!pc) {
//            console.error('no peerconnection');
//            return;
//        }
        if(PC == null){
            Log.i("Error","no peerconnection");
            return;
        }


        if(candidate.sdp == null){
            PC.addIceCandidate(null);
            Log.i("candidate","null");
        }
        else{
            PC.addIceCandidate(candidate);
            Log.i("candidate","not null");

        }

//        if (!candidate.candidate) {
//            await pc.addIceCandidate(null);
//        } else {
//            await pc.addIceCandidate(candidate);
//        }
    }

}
