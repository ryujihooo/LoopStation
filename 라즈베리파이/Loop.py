import paho.mqtt.client as mqtt
import time
from pydub import AudioSegment
import os
from picamera import PiCamera
import cv2

song = AudioSegment.from_file('/home/pi/Desktop/source.3gp')
song = song-100
num=0
camera = PiCamera()
button = [0,0,0,0,0,0,0,0,0]

def camera_capture(n):
    camera.start_preview()
    time.sleep(1)
    camera.capture('/home/pi/Desktop/carmera{}.jpg'.format(n))
    camera.stop_preview()
    src = cv2.imread('/home/pi/Desktop/carmera.jpg',cv2.IMREAD_COLOR)
    srcn = cv2.imread('/home/pi/Desktop/carmera{}.jpg'.format(n),cv2.IMREAD_COLOR)
    srcn = cv2.resize(srcn,dsize=(100,100),interpolation=cv2.INTER_AREA)
    r, c = n/3, n%3
    roi = src[r:r+100,c:c+100]
    roi = srcn.copy()
    
def display_image():
    pass

def on_connect(client,userdata,flags,rc):
    client.subscribe("SOURCE")
    
def on_message(client,userdata,msg):
    global song
    global num
    num+=1
    print("message arrive")
    fw=open('temp.3gp','wb')
    #fw=open('source.3gp','wb')
    fw.write(msg.payload)
    fw.close()
    
    data = AudioSegment.from_file('/home/pi/Desktop/temp.3gp',formet='arm')
    song  = song.overlay(data*2)
    song.export('/home/pi/Desktop/source.3gp')
    fr=open('source.3gp','rb')
    sendData = fr.read()
    fr.close()
    mqttClient.publish("CTRL-SPEAKER",sendData)
    #mqttClient.publish("CTRL-SPEAKER",msg.payload)

def mikeOn():
    print("MIKE ON")
    mqttClient.publish("CTRL-MIKE","MIKE ON")

def mikeOff():
    print("MIKE OFF")
    mqttClient.publish("CTRL-MIKE","MIKE OFF")
    
mqttClient = mqtt.Client("LoopStation")
mqttClient.on_connect=on_connect
mqttClient.on_message = on_message
mqttClient.connect("localhost",1883)

mqttClient.loop_start()

button = bytearray(button)
waitEnd = 0
print('start')
readBtn = os.open('/dev/fpga_push_switch',os.O_RDONLY)
writeLed = os.open('/dev/fpga_led',os.O_WRONLY)

ret = os.read(readBtn,9)
print(ret[0])
ledInput = bytes([0])
while True:
    ret = os.read(readBtn,9)
    os.write(writeLed,ledInput)
    #print(ret)
    if waitEnd==0:
        if(ret[0]==1):
            mikeOn()
            waitEnd = 1
            ledInput = bytes([128])
            #camera_capture(1)
            #display_image()
            time.sleep(1)
    else:
        if(ret[0]==1)and(waitEnd==1):
            mikeOff()
            ledInput=bytes([0])
            os.write(writeLed,ledInput)
            waitEnd = 0
            time.sleep(1)