# FM AntiCheat Dataset Format

Header: FMAC-DATASET-v1
Label: int (0 legit, 1 cheat)
Created At: long epoch millis
Frames: int

Each frame contains:
- tick: long
- feature length: int
- feature vector: float[]
- yaw delta: float
- pitch delta: float
- cps: float
- target distance: float
- tracking consistency: float
- smoothness: float

The target dataset size is configured to approximately 17 KB by default for quick iterative collection on live servers.
The format is intentionally compact and suitable for both direct in-server training and offline conversion.
