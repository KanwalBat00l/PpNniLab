import argparse
import numpy as np
import os
import sys

def convert_np_to_fixedpt(np_file, scale=12, output_file=None):
    if not os.path.isfile(np_file):
        sys.exit(f"Numpy file {np_file} does not exist")
    np_array = np.load(np_file, allow_pickle=False)
    if output_file is None:
        output_file = os.path.splitext(np_file)[0] + f"_fixedpt_scale_{scale}.inp"
    with open(output_file, "w") as f:
        for x in np.nditer(np_array, order="C"):
            f.write(str(int(x * (1 << scale))) + " ")
        f.write("\n")
    print(f"Fixed-point output saved in {output_file}")
    return output_file

if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="Convert .npy to fixed-point .inp")
    parser.add_argument("--npy", required=True, type=str, help="Path to .npy input")
    parser.add_argument("--scale", required=False, type=int, default=12, help="Scaling factor")
    parser.add_argument("--output", required=False, type=str, help="Output .inp file")
    args = parser.parse_args()

    convert_np_to_fixedpt(args.npy, args.scale, args.output)
