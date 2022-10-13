import paho.mqtt.client as mqtt
import time
from pydub import AudioSegment
import os
from picamera import PiCamera
import cv2
import numpy as np

song = AudioSegment.from_file('/home/pi/Desktop/source.3gp')
song = song-100
num=0
camera = PiCamera()
button = [0,0,0,0,0,0,0,0,0]
src = np.zeros((300,300,3),np.uint8)
cv2.imwrite('/home/pi/Desktop/carmera.jpg',src)

def camera_capture(n):
    global src
    camera.start_preview()
    time.sleep(1)
    camera.capture('/home/pi/Desktop/carmera{}.jpg'.format(n))
    camera.stop_preview()
    src = cv2.imread('/home/pi/Desktop/carmera.jpg',cv2.IMREAD_COLOR)
    srcn = cv2.imread('/home/pi/Desktop/carmera{}.jpg'.format(n),cv2.IMREAD_COLOR)
    srcn = cv2.resize(srcn,dsize=(100,100),interpolation=cv2.INTER_AREA)
    r, c = (n-1)/3, (n-1)%3
    roi = src[int(r*100):int((r+1)*100),int(c*100):int((c+1)*100),:]
    roi[:,:,:]=srcn[:,:,:]
    cv2.imwrite('/home/pi/Desktop/carmera.jpg',src)
    
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

def recordOn(n,led):
    mikeOn()
    ledInput = bytes([led])
    os.write(writeLed,ledInput)
    stepInput = bytes([1, 0, 255])
    os.write(writeStep,stepInput)
    camera_capture(n)
    #display_image()
    time.sleep(1)

def recordOff():
    mikeOff()
    ledInput=bytes([0])
    os.write(writeLed,ledInput)
    stepInput = bytes([0, 0, 255])
    os.write(writeStep,stepInput)
    time.sleep(1)
    
mqttClient = mqtt.Client("LoopStation")
mqttClient.on_connect=on_connect
mqttClient.on_message = on_message
mqttClient.connect("localhost",1883)

mqttClient.loop_start()

button = bytearray(button)
waitEnd = 0
print('start')
readBtn = os.open('/dev/fpga_push_switch',os.O_RDONLY)
writeStep = os.open('/dev/fpga_step_motor',os.O_WRONLY)
writeLed = os.open('/dev/fpga_led',os.O_WRONLY)

ret = os.read(readBtn,9)
print(ret[0])
ledInput = bytes([0])
stepInput = bytes([1, 0, 255])
while True:
    ret = os.read(readBtn,9)
    print(ret)
    sleep(1)
    if waitEnd==0:
        if(ret[0]==1):
            recordOn(1,128)
            waitEnd = 1
        elif (ret[1]==1):
            recordOn(2,64)
            waitEnd = 2
        elif (ret[2]==1):
            recordOn(3,32)
            waitEnd = 3
        elif (ret[3]==1):
            recordOn(4,16)
            waitEnd = 4
        elif (ret[4]==1):
            recordOn(5,8)
            waitEnd = 5
        elif (ret[5]==1):
            recordOn(6,4)
            waitEnd = 6
        elif (ret[6]==1):
            recordOn(7,2)
            waitEnd = 7
        elif (ret[7]==1):
            recordOn(8,1)
            waitEnd = 8
    else:
        if(ret[0]==1)and(waitEnd==1):
            waitEnd = 0
            recordOff()
        if(ret[1]==1)and(waitEnd==2):
            waitEnd = 0
            recordOff()
        if(ret[2]==1)and(waitEnd==3):
            waitEnd = 0
            recordOff()
        if(ret[3]==1)and(waitEnd==4):
            waitEnd = 0
            recordOff()
        if(ret[4]==1)and(waitEnd==5):
            waitEnd = 0
            recordOff()
        if(ret[5]==1)and(waitEnd==6):
            waitEnd = 0
            recordOff()
        if(ret[6]==1)and(waitEnd==7):
            waitEnd = 0
            recordOff()
        if(ret[7]==1)and(waitEnd==8):
            waitEnd = 0
            recordOff()