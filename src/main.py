import numpy as np
import cv2
import argparse

def loadVideoFromFile(filePath):
    cap = cv2.VideoCapture(filePath)
    return cap


def loadImageFromFile(filePath):
    image = cv2.imread(filePath,1)
    return image



