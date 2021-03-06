import re
import cv2
from tflite_runtime.interpreter import Interpreter
import numpy as np
import pytesseract
from PIL import Image, ImageFont, ImageDraw
from io import BytesIO
from pytesseract import Output
from bluetooth import *
import json
import requests
import sys

LIMIT_PX = 1024
LIMIT_BYTE = 1024*1024  # 1MB
LIMIT_BOX = 40

rest_api_key = 'b824eaf2d3058fd347ad3106059183d9'

CAMERA_WIDTH = 1080
CAMERA_HEIGHT = 810
VIDEO_NAME = "v6_best.mp4"

def load_labels(path='labels_many.txt'):
  """Loads the labels file. Supports files with or without index numbers."""
  with open(path, 'r', encoding='utf-8') as f:
    lines = f.readlines()
    labels = {}
    for row_number, content in enumerate(lines):
      pair = re.split(r'[:\s]+', content.strip(), maxsplit=1)
      if len(pair) == 2 and pair[0].strip().isdigit():
        labels[int(pair[0])] = pair[1].strip()
      else:
        labels[row_number] = pair[0].strip()
  return labels

def set_input_tensor(interpreter, image):
  """Sets the input tensor."""
  tensor_index = interpreter.get_input_details()[0]['index']
  input_tensor = interpreter.tensor(tensor_index)()[0]
  input_tensor[:, :] = np.expand_dims((image-255)/255, axis=0)


def get_output_tensor(interpreter, index):
  """Returns the output tensor at the given index."""
  output_details = interpreter.get_output_details()[index]
  tensor = np.squeeze(interpreter.get_tensor(output_details['index']))
  return tensor


def detect_objects(interpreter, image, threshold):
  """Returns a list of detection results, each a dictionary of object info."""
  set_input_tensor(interpreter, image)
  interpreter.invoke()
  # Get all output details
  boxes = get_output_tensor(interpreter, 0)
  classes = get_output_tensor(interpreter, 1)
  scores = get_output_tensor(interpreter, 2)
  count = int(get_output_tensor(interpreter, 3))

  results = []
  class_id = []
  
  for i in range(count):
    if scores[i] >= threshold and classes[i] not in class_id:
        result = {
            'bounding_box': boxes[i],
            'class_id': classes[i],
            'score': scores[i]
        }
        results.append(result)
        class_id.append(classes[i])
  return results

def pointInRect(point,rect):
    x1, y1, w, h = rect
    x2 = x1 + w
    y2 = y1 + h
    x, y = point
    if (x1 < x and x < x2):
        if (y1 < y and y < y2):
            return True
    return False

def kakao_ocr_resize(image_path: str):
    """
    ocr detect/recognize api helper
    ocr api??? ??????????????? ???????????? ???????????? ?????? ????????? ???????????? ??????.

    pixel ???????????? ??????: resize
    ?????? ???????????? ??????  : ?????? ???????????? ??????, ????????? ?????? ?????? ?????? ??????. (???????????? ???????????? ??????)

    :param image_path: ??????????????? ??????
    :return:
    """
    image = cv2.imread(image_path)
    height, width, _ = image.shape

    if LIMIT_PX < height or LIMIT_PX < width:
        ratio = float(LIMIT_PX) / max(height, width)
        image = cv2.resize(image, None, fx=ratio, fy=ratio)
        height, width, _ = height, width, _ = image.shape

        # api ???????????? ???????????? resize??? ??????, recognize??? resize??? ????????? ???????????????.
        image_path = "{}_resized.jpg".format(image_path)
        cv2.imwrite(image_path, image)

        return image_path
    return None

def kakao_ocr(image_path: str, appkey: str):
    """
    OCR api request example
    :param image_path: ??????????????? ??????
    :param appkey: ????????? ??? REST API ???
    """
    API_URL = 'https://dapi.kakao.com/v2/vision/text/ocr'

    headers = {'Authorization': 'KakaoAK {}'.format(appkey)}

    image = cv2.imread(image_path)
    jpeg_image = cv2.imencode(".jpg", image)[1]
    data = jpeg_image.tobytes()


    return requests.post(API_URL, headers=headers, files={"image": data})

server_socket= BluetoothSocket(RFCOMM)

port = 1
server_socket.bind(("", port))
server_socket.listen(5)

client_socket, address = server_socket.accept()
print("Accepted connection from ", address)
client_socket.send("bluetooth connected!")

while True:
    """?????? ?????????, ????????????  """
    dt_image = []
    dt_result = []
          
    """?????? ?????? ?????? """
    selected = False
    
    """?????? 5????????? ????????? ?????? ??????????????? ????????????  """
    count = 0
    no_dt = 0
    
    
    """ ????????? ????????? ??????  """
    text_num = 0
    
    """????????? ?????? ???????????? ?????? """
    sleep = False
    
    """????????? ?????? """
    sign_num = 0
    
    labels = load_labels()
    interpreter = Interpreter('detect_5000.tflite')
    interpreter.allocate_tensors()
    _, input_height, input_width, _ = interpreter.get_input_details()[0]['shape']

    cap = cv2.VideoCapture(VIDEO_NAME)
    #print(cap.get(cv2.CAP_PROP_FRAME_WIDTH))
    #print(cap.get(cv2.CAP_PROP_FRAME_HEIGHT))
    while cap.isOpened():
        ret, frame = cap.read()
        if int(cap.get(cv2.CAP_PROP_POS_FRAMES))%10==0:
            img = cv2.resize(cv2.cvtColor(frame, cv2.COLOR_BGR2RGB), (320,320))
            res = detect_objects(interpreter, img, 0.5)
            if(len(res)!=0):
                print(res)
            
            """?????? ??????????????? ?????? ?????? ?????? count"""
            if len(res)==0:
                no_dt = no_dt+1
            
            """?????? ?????? ??????"""
            temp_result=[]
            temp_image=[]
            temp_box=[]
            
            for result in res:
                
                ymin, xmin, ymax, xmax = result['bounding_box']
                xmin = int(max(1,xmin * CAMERA_WIDTH))
                xmax = int(min(CAMERA_WIDTH, xmax * CAMERA_WIDTH))
                ymin = int(max(1, ymin * CAMERA_HEIGHT))
                ymax = int(min(CAMERA_HEIGHT, ymax * CAMERA_HEIGHT))
                
                cv2.rectangle(frame,(xmin, ymin),(xmax, ymax),(0,255,0),3)
                cv2.putText(frame,labels[int(result['class_id'])],(xmin, min(ymax, CAMERA_HEIGHT-20)), cv2.FONT_HERSHEY_SIMPLEX, 0.5,(255,255,255),2,cv2.LINE_AA) 
                    
                """?????? ??????"""
                temp_result.append(result)
                temp_image.append(frame[ymin: ymax, xmin: xmax])
                
                """??????????????? ?????? ????????? ????????? ?????? ???"""
                if int(result['class_id'])==0 and (ymax-ymin)> CAMERA_WIDTH/6 and (ymax-ymin)< CAMERA_WIDTH/3.5:
                    selected=True
                """ """
            cv2.imshow('Pi Feed', frame)
            
            if sleep == True:
                if(no_dt>3):
                    sleep = False
            elif not dt_image and count==0:
                if selected == True:
                    for temp in temp_result:
                        dt_result.append(temp)
                    for temp in temp_image:   
                        dt_image.append(temp)
                    count=count+1
                    print(count)
                    no_dt = 0         
                
            elif count >=5 :#or (count != 0 and no_dt >= 2):#count != 0 and no_dt >= 1 :
                #global result_image
                text_pt = [[],[]]
                rect = []
                arrow = []
                z=0
                sign_num = sign_num + 1
                for image,result in zip(dt_image, dt_result):
                    #cv2.imshow('result'+ str(i), image)
                    
                    if(int(result['class_id'])==0):
                        
                        """?????? ????????? """
                        if(len(dt_image)>2):
                            height, width= int(image.shape[0]), int(image.shape[1]/(len(dt_image)-1))
                            x = 0
                            y = 0
                            for i in range(1, len(dt_image)): 
                                cv2.rectangle(image, (x, y), (x + width, y + height), (255, 0, 255), 2)
                                rect.append([x, y, x + width, y + height])
                                x = x + width
                        else :
                            height, width= int(image.shape[0]), int(image.shape[1])
                            cv2.rectangle(image, (0, 0), (0 + width, 0 + height), (255, 0, 255), 2)
                            rect.append([0, 0, width, height])
                        """????????? ????????? """
                        cv2.imwrite('kakaoimg.jpg', image)   
                        image_path = 'kakaoimg.jpg'
                        resize_impath = kakao_ocr_resize(image_path)
                        if resize_impath is not None:
                            image_path = resize_impath
                            print("?????? ?????? ???????????? ??? ???????????? ???????????????")
                        
                        #print("[OCR] output:\n{}\n".format(json.dumps(output, sort_keys=True, indent=2, ensure_ascii=False)))
                        output = kakao_ocr(image_path, rest_api_key).json()
                        outputdata = json.dumps(output, ensure_ascii=False,sort_keys=True, indent=2)
                        print("[OCR] output:\n{}\n".format(outputdata))
                        
                        #?????? ????????? array??? ??????
                        outputdata = json.loads(outputdata)

                        for i in range(len(outputdata['result'])):
                        #box ???????????? ????????? ???????????? 
                            x = outputdata['result'][i]['boxes'][0][0]
                            y = outputdata['result'][i]['boxes'][0][1]
                            w =  (outputdata['result'][i]['boxes'][1][0] -  outputdata['result'][i]['boxes'][0][0])
                            h =  (outputdata['result'][i]['boxes'][2][1] -  outputdata['result'][i]['boxes'][0][1])
                            cv2.rectangle(image, (x, y), (x + w, y + h), (0, 0, 255), 2)
                            text = outputdata['result'][i]['recognition_words'][0]
                            hangul = re.compile(r'[???-??????-???0-9]')
                            #hangul = re.compile(r'[???-??????-???a-zA-Z]')
                            text_kr = re.findall(hangul, text)
                            for j in range(len(rect)) :
                                point = (2*x+w) /2, (2*y+h) /2
                                if(pointInRect(point ,rect[j])==True):
                                    text_pt[j].append(text_kr)
                        cv2.imshow('detect_sign', image)
                    elif (int(result['class_id'])==1):
                        arrow.append(['????????? ?????? ',result['bounding_box'][1]])
                        z=z+1
                    elif (int(result['class_id'])==2):
                        arrow.append(['?????? ?????? ',result['bounding_box'][1]])
                        z=z+1
                    elif (int(result['class_id'])==3):
                        arrow.append(['???????????? ?????? ',result['bounding_box'][1]])
                        z=z+1
                    elif (int(result['class_id'])==4):
                        arrow.append(['??????????????? ?????? ',result['bounding_box'][1]])
                        z=z+1
                    elif (int(result['class_id'])==5):
                        arrow.append(['???????????? ',result['bounding_box'][1]])
                        z=z+1
                    elif (int(result['class_id'])==6):
                        arrow.append(['????????? ???????????? ?????? ',result['bounding_box'][1]])
                        z=z+1
                    elif (int(result['class_id'])==7):
                        arrow.append(['????????? ??????????????? ?????? ',result['bounding_box'][1]])
                        z=z+1
                
                arrow.sort(key=lambda x:x[1])
                """?????? ?????? ?????? """
                tts = ''
                for k in range(0, len(rect)) :
                    if(len(arrow[k][0])!=0):
                        tts = tts + str(arrow[k][0])
                    for x in range(0, len(text_pt[k])) :
                        for y in range(0, len(text_pt[k][x])) :
                            #text_pt[k][x][y] = re.sub("\???|\???|\??????|\3|\???","",text_pt[k][x][y])
                            text_pt[k][x][y]=text_pt[k][x][y].replace('???','').replace('???','').replace('??????','').replace('3','')
                            if(bool(re.search(r'\d', text_pt[k][x][y]))==True):
                                tts = tts + text_pt[k][x][y] + '??? ??????'
                            else :
                                tts = tts + text_pt[k][x][y]
                        tts = tts + ' '
                    if(k==len(rect)-1):
                        tts = tts + '?????????.'
                    else:
                        print(', ')
                        tts = tts + ', '
                print(tts)
                client_socket.send(tts)
                
                print('????????? ?????? ??????')
                dt_image = [] 
                dt_result = []
                count = 0
                print(count)
                text_num = 0
                sleep = True
                
                
            elif selected == True and len(temp_result)>=len(dt_result):# and len(findText(frame[ymin: ymax, xmin: xmax])) > text_num:
                dt_image = [] 
                dt_result = []
                for temp in temp_result:
                    dt_result.append(temp)
                for temp in temp_image:   
                    dt_image.append(temp)
                if(count==2):
                    print('???????????? ?????????????????????. ?????? ???????????????.')
                    client_socket.send('???????????? ?????????????????????. ?????? ???????????????.')
                count=count+1
                print(count)
                no_dt = 0
                selected=False
        if cv2.waitKey(10) & 0xFF ==ord('q'):
            cap.release()
            cv2.destroyAllWindows()

        if cv2.waitKey(10) & 0xFF ==ord('q'):
            cap.release()
            cv2.destroyAllWindows()

client_socket.close()
server_socket.close()