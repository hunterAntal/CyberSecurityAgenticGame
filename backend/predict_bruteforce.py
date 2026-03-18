#!/usr/bin/env python3
"""Run brute-force RF model on a hardcoded attack-indicative sample and print probability."""
import sys
import warnings
import joblib

# CICIDS-2017 standard feature names (well-documented public dataset)
CICIDS_FEATURES = [
    "Destination Port", "Flow Duration", "Total Fwd Packets",
    "Total Backward Packets", "Total Length of Fwd Packets",
    "Total Length of Bwd Packets", "Fwd Packet Length Max",
    "Fwd Packet Length Min", "Fwd Packet Length Mean", "Fwd Packet Length Std",
    "Bwd Packet Length Max", "Bwd Packet Length Min", "Bwd Packet Length Mean",
    "Bwd Packet Length Std", "Flow Bytes/s", "Flow Packets/s",
    "Flow IAT Mean", "Flow IAT Std", "Flow IAT Max", "Flow IAT Min",
    "Fwd IAT Total", "Fwd IAT Mean", "Fwd IAT Std", "Fwd IAT Max", "Fwd IAT Min",
    "Bwd IAT Total", "Bwd IAT Mean", "Bwd IAT Std", "Bwd IAT Max", "Bwd IAT Min",
    "Fwd PSH Flags", "Bwd PSH Flags", "Fwd URG Flags", "Bwd URG Flags",
    "Fwd Header Length", "Bwd Header Length", "Fwd Packets/s", "Bwd Packets/s",
    "Min Packet Length", "Max Packet Length", "Packet Length Mean",
    "Packet Length Std", "Packet Length Variance", "FIN Flag Count",
    "SYN Flag Count", "RST Flag Count", "PSH Flag Count", "ACK Flag Count",
    "URG Flag Count", "CWE Flag Count", "ECE Flag Count", "Down/Up Ratio",
    "Average Packet Size", "Avg Fwd Segment Size", "Avg Bwd Segment Size",
    "Fwd Header Length.1", "Subflow Fwd Packets", "Subflow Fwd Bytes",
    "Subflow Bwd Packets", "Subflow Bwd Bytes", "Init_Win_bytes_forward",
    "Init_Win_bytes_backward", "act_data_pkt_fwd", "min_seg_size_forward",
    "Active Mean", "Active Std", "Active Max", "Active Min",
    "Idle Mean", "Idle Std", "Idle Max", "Idle Min",
]

# Attack-indicative brute-force sample:
# Short flow, high packet rate, repeated SSH port, many SYN flags
ATTACK_SAMPLE = {
    "Destination Port": 22,         # SSH target
    "Flow Duration": 50000,         # Very short flow (50ms)
    "Total Fwd Packets": 60,        # Many forward packets
    "Total Backward Packets": 60,   # Many backward packets
    "Total Length of Fwd Packets": 3600,
    "Total Length of Bwd Packets": 3600,
    "Fwd Packet Length Max": 64,
    "Fwd Packet Length Min": 40,
    "Fwd Packet Length Mean": 54.0,
    "Fwd Packet Length Std": 8.0,
    "Bwd Packet Length Max": 64,
    "Bwd Packet Length Min": 40,
    "Bwd Packet Length Mean": 54.0,
    "Bwd Packet Length Std": 8.0,
    "Flow Bytes/s": 144000.0,       # Very high bytes/s
    "Flow Packets/s": 2400.0,       # Very high packets/s
    "Flow IAT Mean": 800.0,
    "Flow IAT Std": 200.0,
    "Flow IAT Max": 1200.0,
    "Flow IAT Min": 100.0,
    "Fwd IAT Total": 48000.0,
    "Fwd IAT Mean": 800.0,
    "Fwd IAT Std": 200.0,
    "Fwd IAT Max": 1200.0,
    "Fwd IAT Min": 100.0,
    "Bwd IAT Total": 48000.0,
    "Bwd IAT Mean": 800.0,
    "Bwd IAT Std": 200.0,
    "Bwd IAT Max": 1200.0,
    "Bwd IAT Min": 100.0,
    "Fwd PSH Flags": 0,
    "Bwd PSH Flags": 0,
    "Fwd URG Flags": 0,
    "Bwd URG Flags": 0,
    "Fwd Header Length": 1200,
    "Bwd Header Length": 1200,
    "Fwd Packets/s": 1200.0,
    "Bwd Packets/s": 1200.0,
    "Min Packet Length": 40,
    "Max Packet Length": 64,
    "Packet Length Mean": 54.0,
    "Packet Length Std": 8.0,
    "Packet Length Variance": 64.0,
    "FIN Flag Count": 0,
    "SYN Flag Count": 60,           # High SYN (repeated connection attempts)
    "RST Flag Count": 0,
    "PSH Flag Count": 0,
    "ACK Flag Count": 60,
    "URG Flag Count": 0,
    "CWE Flag Count": 0,
    "ECE Flag Count": 0,
    "Down/Up Ratio": 1.0,
    "Average Packet Size": 54.0,
    "Avg Fwd Segment Size": 54.0,
    "Avg Bwd Segment Size": 54.0,
    "Fwd Header Length.1": 1200,
    "Subflow Fwd Packets": 60,
    "Subflow Fwd Bytes": 3600,
    "Subflow Bwd Packets": 60,
    "Subflow Bwd Bytes": 3600,
    "Init_Win_bytes_forward": 65535,
    "Init_Win_bytes_backward": 65535,
    "act_data_pkt_fwd": 60,
    "min_seg_size_forward": 20,
    "Active Mean": 0.0,
    "Active Std": 0.0,
    "Active Max": 0.0,
    "Active Min": 0.0,
    "Idle Mean": 0.0,
    "Idle Std": 0.0,
    "Idle Max": 0.0,
    "Idle Min": 0.0,
}

def main():
    try:
        model = joblib.load("Brute_Force Agent/Brute_force_model.pkl")

        # Get actual feature names from model if available
        try:
            feature_names = list(model.feature_names_in_)
        except AttributeError:
            # Fall back to standard CICIDS features sized to model input
            n = model.n_features_in_ if hasattr(model, "n_features_in_") else len(CICIDS_FEATURES)
            feature_names = CICIDS_FEATURES[:n]

        X = [[ATTACK_SAMPLE.get(f, 0.0) for f in feature_names]]
        with warnings.catch_warnings():
            warnings.simplefilter("ignore")
            proba = model.predict_proba(X)[0]
        classes = list(model.classes_)

        # Find attack class — look for non-BENIGN label
        attack_idx = 0
        for i, c in enumerate(classes):
            label = str(c).upper()
            if "BENIGN" not in label and label not in ("0", "NORMAL"):
                attack_idx = i
                break

        print(proba[attack_idx])
    except Exception as e:
        print(f"[predict_bruteforce] error: {e}", file=sys.stderr)
        print(0.0)

if __name__ == "__main__":
    main()
