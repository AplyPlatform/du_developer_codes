# DronePlay Codes

## 경로설명
+ [/api/web](https://github.com/theknightsfield/droneplaycodes/tree/master/api/web) : Open API를 활용하는 웹 기반의 예제 코드.
+ [/api/android](https://github.com/theknightsfield/droneplaycodes/tree/master/api/android) : Open API를 활용하는 안드로이드 기반의 예제 코드.

# DronePlayMission Code - 안드로이드 
<img src="/api/screen.png" alt="DronePlayMission" width="300">
* 지도를 터치하여 마크가 된 모든 장소로 드론이 방문하는 코드 예제 입니다.
  * 각 장소를 방문할때 마다 해당 장소의 좌표를 DronePlay API를 호출하여 기록합니다.
  * 코드의 수행을 위해 Manifest 파일에 구글 지도 API KEY, DJI SDK API KEY, DRONEPLAY API TOKEN 정보를 입력해야 합니다.

> AndroidManifest.xml
<pre>
<code>
... meta-data
           android:name="com.google.android.geo.API_KEY"
           android:value="GOOGLE-MAP-API-KEY" ...
... meta-data
           android:name="com.dji.sdk.API_KEY"
           android:value="DJI-SDK-API-KEY" ...

... meta-data
           android:name="io.droneplay.token"
           android:value="DRONEPLAY-API-TOKEN" ...

... meta-data
           android:name="io.droneplay.email"
           android:value="DRONEPLAY-API-EMAIL" ...
</code>
</pre>

# 웹 
> [DronePlay Samples](http://dev.droneplay.io/dev/examples/index.html) : Open API를 활용한 웹 예제가 실행되는 모습을 볼 수 있습니다.
