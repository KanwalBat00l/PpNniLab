import argparse
import numpy as np
from PIL import Image
import os
import sys

def preprocess_image_to_npy(input_img, dtype='float32', target_size=(224, 224)):
    """Convert image to numpy array in CHW format"""
    if not os.path.isfile(input_img):
        sys.exit(f"Input image {input_img} does not exist")
    img = Image.open(input_img).resize(target_size)
    arr = np.array(img).astype(dtype) / 255.0
    arr = arr.transpose(2, 0, 1)  # CHW format
    arr = arr[np.newaxis, :, :, :]  # Add batch dimension [1, C, H, W]
    return arr

def save_npy(np_array, output_npy):
    np.save(output_npy, np_array)
    print(f"Saved numpy array to {output_npy} with shape {np_array.shape} and dtype {np_array.dtype}")

if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="Convert image to .npy array")
    parser.add_argument("input_img", type=str, help="Path to input image (png/jpg)")
    parser.add_argument("output_npy", type=str, help="Path to output .npy file")
    parser.add_argument("--dtype", type=str, default="float32", help="Data type for numpy array")
    args = parser.parse_args()

    np_array = preprocess_image_to_npy(args.input_img, dtype=args.dtype)
    save_npy(np_array, args.output_npy)
