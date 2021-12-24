# 길잡이 🚇

**시각장애인을 위한 지하철 내비게이션**

> **🏆 2021 프로보노 공모전 입선작**  
> **프로젝트 기간 : 2021.04.01 ~ 2021.06.16**  

<br>

## 프로젝트 소개
<img src="https://user-images.githubusercontent.com/77331348/147336529-d42adb39-fbcf-4338-8f8f-bc2904451013.png" width="300">

```
본 서비스는 시각장애인의 원활한 지하철 이용을 위한 지하철 역내 경로 안내와 지하철 탑승 간 하차 알림 서비스이다. 

인공지능, IoT, 영상처리 등의 IT 기술 발달로 인적자원의 업무를 소프트웨어가 대체하면서 다양한 수익이 창출되고 있는 현재 
수익성이 결여된 사회적 약자를 위한 인적자원 대체 소프트웨어는 부족한 실정이다. 

실내에서의 GPS 한계를 딥러닝을 통해 극복하고 시각장애인이라는 특수한 사용자의 편의성을 위해 임베디드 디바이스와 음성을 적극 활용하여 
트렌드에 맞는 IT 코어 기술 기반의 소프트웨어로 사회적 약자의 권리를 보장하고자 한다.
```

<br>

## 프로젝트 구성도
<img src="https://user-images.githubusercontent.com/77331348/147336537-7bbb877e-7539-4ee2-afd2-9f5230734709.png">

* 프로젝트는 안드로이드 스마트폰과 라즈베리파이 디바이스로 구성되며 안드로이드 스마트폰에서는 음성출력, 위치정보를, 라즈베리파이 디바이스에서는 영상촬영과 표지판 검출 및 텍스트 변환을 처리하며 두 기기는 블루투스 통신을 통해 연결된다.

* 안드로이드 스마트폰에서의 현재 위치 정보를 기반으로 근처의 지하철역 검색, 사용자의 지하철 탑승 중의 하차정보를 음성을 통해 알려준다. 라즈베리파이에서의 촬영된 영상을 딥러닝을 통한 표지판 객체 탐지 모델을 통해 입력 영상과 표지판을 분리하고, 분리된 표지판 영상 내의 방향 정보인 화살표를 다시 탐지하고 글자는 OCR을 통해 텍스트로 변환한다. 표지판 영상으로부터 생성된 정보는 블루투스 통신을 통해 스마트폰으로 전달되어 음성으로 사용자에게 알려준다.

<br>

## 개발 환경

<table style="border-collapse: collapse; width: 100%;" border="1" data-ke-align="alignLeft">
<tbody>
<tr>
<td colspan="2"><span><span>구분</span></span></td>
<td><span><span>상세 내용</span></span></td>
</tr>
<tr>
<td rowspan="4"><span><span>S/W</span></span></td>
<td><span><span>OS</span></span></td>
<td><span><span>Window 10 Pro, Home</span></span><br /><span><span>Raspberry Pi OS</span></span></td>
</tr>
<tr>
<td><span><span>개발환경</span></span></td>
<td><span><span>Android Studio 4.2.1</span></span><br /><span><span>Thonny Python IDE</span></span><br /><span><span>Google Colab</span></span></td>
</tr>
<tr>
<td><span><span>개발도구</span></span></td>
<td><span><span>Tenserflow Lite</span></span><br /><span><span>Kakao Vision OCR API,</span></span><br /><span><span>Kakao Map API,</span></span><br /><span><span>Kakao Local API,</span></span><br /><span><span>Kakao Synthesize API</span></span></td>
</tr>
<tr>
<td><span><span>개발언어</span></span></td>
<td><span><span>Java</span></span><br /><span><span>Python 3.7.2, 3.9.2</span></span></td>
</tr>
<tr>
<td rowspan="2"><span><span>H/W</span></span></td>
<td><span><span>디바이스</span></span></td>
<td><span><span>Galaxy S6(Android 7.0, </span><span>엑시노스</span><span>7420, 3GB RAM, Bluetooth 4.1+LE)</span></span><br /><span><span>RaspberryPi 4B(1.5GHz CPU, 4GB RAM, Bluetooth 5.0)</span></span><br /><span><span>Embeded Camera(QHD, 500</span><span>만 화소</span><span>)</span></span></td>
</tr>
<tr>
<td><span><span>통신</span></span></td>
<td><span><span>Bluetooth</span></span></td>
</tr>
</tbody>
</table>

<br>

## 주요 기능

### 1️⃣ 지하철 역 내 경로 안내

<img src="https://user-images.githubusercontent.com/77331348/147336540-67d62069-8696-40d0-921d-40c4e70a2cca.png">

: 카메라를 통해 영상을 촬영하면서 실시간으로 지하철 내 안내 표지판, 화살표, 문자를 인식하여 경로 정보를 생성하고 음성으로 알려준다.

<img src="https://user-images.githubusercontent.com/77331348/147336544-c06b2b53-172a-4703-91a1-6f265870decc.png" width="450"> <img src="https://user-images.githubusercontent.com/77331348/147336547-149e1b47-60a5-44cb-a3bd-5c0e1b4f10f5.png" width="400">
```
① 라즈베리 파이에 부착된 카메라를 통해 지하철 내부 영상을 촬영
② 지하철 안내 표지판, 화살표 인식
③ 표지판 인식 메시지를 안드로이드에 전송
④ 전송 받은 표지판 인식 메시지 읽어 주기
⑤ 안내 표지판 내 글자 인식
⑥ 화살표 개수, 글자 위치에 따른 경로 정보 생성
⑦ 생성된 경로 정보를 안드로이드에 전송
⑧ 전송 받은 경로 정보 읽어 주기
```
### 2️⃣ 지하철 탑승 시 현재 위치 안내

<img src="https://user-images.githubusercontent.com/77331348/147336550-1b573773-9e11-4153-a621-f845f987ddb0.png">

: 지하철 탑승 시에는 GPS를 통해 현재 자신의 위치를 파악하고 자신의 위치에서 가장 가까운 역을 음성으로 알려준다.

<img src="https://user-images.githubusercontent.com/77331348/147336551-9c8f71f5-a855-443a-9362-06a57b883f99.png" width="400">

```
① GPS로 현재 자신의 위치를 파악
② 자신의 위치에서 가장 가까운 역 검색
③ 화면 클릭 시, 가장 가까운 역 및 역까지 남은 거리 읽어 주기
```

## 적용 기술
```
> Tenserflow lite
> Kakao Vision OCR API
> Blue tooth
> Kakao Map
> Local API 
> TTS (Text To Speech)
```
<br>

## 기대효과 및 활용 방안

* 기대효과
```
1. 시각 장애인의 이동성의 권리 보장
2. 무료 앱 서비스 배포를 통해 경제적 부담 감소
3. 음성 인터페이스를 통한 쉬운 사용성
```
* 활용 방안
```
1. 여러 공공 시설로 서비스 확대
2. 노약자, 어린이 등 사용자 대상 확대
3. 신체 부착형 웨어러블 기기를 통한 자유도 증가
```
