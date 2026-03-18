#!/usr/bin/env python3
"""Run phishing RF model on a hardcoded attack-indicative sample and print probability."""
import sys
import warnings
import joblib

def main():
    try:
        model = joblib.load("Phishing_Agent/phishing_model.pkl")

        # Attack-indicative phishing sample
        sample = {
            "PctExtHyperlinks": 0.85,
            "PctExtNullSelfRedirectHyperlinksRT": 0.90,
            "FrequentDomainNameMismatch": 1,
            "PctExtResourceUrls": 0.80,
            "PctNullSelfRedirectHyperlinks": 0.70,
            "NumDash": 5,
            "ExtMetaScriptLinkRT": 0.75,
            "SubmitInfoToEmail": 1,
            "InsecureForms": 1,
        }

        # Get feature names from model if available
        try:
            feature_names = list(model.feature_names_in_)
        except AttributeError:
            feature_names = list(sample.keys())

        X = [[sample.get(f, 0) for f in feature_names]]
        with warnings.catch_warnings():
            warnings.simplefilter("ignore")
            proba = model.predict_proba(X)[0]
        classes = list(model.classes_)

        # Class 1 is attack (phishing), class 0 is benign
        attack_idx = classes.index(1) if 1 in classes else len(classes) - 1
        print(proba[attack_idx])
    except Exception as e:
        print(f"[predict_phishing] error: {e}", file=sys.stderr)
        print(0.0)

if __name__ == "__main__":
    main()
