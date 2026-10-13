#!/usr/bin/env python3
"""
Compares the OBL-Enhanced HGA-SSA model against the baseline results
reported in the ICTCS 2025 research paper (Tables V & VI).

Benchmarks from Paper:
  - Standalone GA      : 134.76 s
  - Standalone SSA     : 172.20 s
  - Baseline HGA-SSA   : 115.60 s
"""

import os
import sys
import pandas as pd

DEFAULT_CSV_PATH = os.path.join(os.path.dirname(__file__), "..", "results", "best_schedule.csv")

# Baseline values reported in Table V of the paper
PAPER_BENCHMARKS = {
    "Genetic Algorithm (GA)": 134.76,
    "Salp Swarm Algorithm (SSA)": 172.20,
    "Baseline HGA-SSA (Paper)": 115.60
}


def compare_models(csv_path=DEFAULT_CSV_PATH):
    if not os.path.exists(csv_path):
        print(f"Error: Schedule file not found at: {csv_path}", file=sys.stderr)
        print("Please run ResultExporter or the simulation first to produce results/best_schedule.csv", file=sys.stderr)
        sys.exit(1)

    df = pd.read_csv(csv_path)

    # Determine makespan from schedule file
    proposed_makespan = float(df["vm_total_time"].max())

    print("\n" + "=" * 78)
    print("   BENCHMARK COMPARISON: PROPOSED MODEL VS. PAPER RESULTS (TABLES V & VI)")
    print("=" * 78)

    comparison_data = []

    for model_name, baseline_makespan in PAPER_BENCHMARKS.items():
        reduction = ((baseline_makespan - proposed_makespan) / baseline_makespan) * 100.0
        comparison_data.append({
            "Model": model_name,
            "Makespan (s)": f"{baseline_makespan:.2f}",
            "Reduction by Our Model (%)": f"{reduction:+.2f}%"
        })

    # Add our proposed model row
    comparison_data.append({
        "Model": "Proposed OBL-HGA-SSA (Ours)",
        "Makespan (s)": f"{proposed_makespan:.2f}",
        "Reduction by Our Model (%)": "Reference (0.00%)"
    })

    comp_df = pd.DataFrame(comparison_data)
    print(comp_df.to_string(index=False))
    print("=" * 78)

    # Paper comparison analysis
    paper_baseline = PAPER_BENCHMARKS["Baseline HGA-SSA (Paper)"]
    improvement_vs_paper = ((paper_baseline - proposed_makespan) / paper_baseline) * 100.0

    if proposed_makespan <= paper_baseline:
        print(f"\n[PASS] Proposed model achieved a makespan of {proposed_makespan:.2f}s,")
        print(f"       outperforming the paper's hybrid baseline of {paper_baseline:.2f}s by {improvement_vs_paper:.2f}%.")
    else:
        print(f"\n[NOTE] Makespan is {proposed_makespan:.2f}s (Baseline was {paper_baseline:.2f}s).")
    print()


if __name__ == "__main__":
    path = sys.argv[1] if len(sys.argv) > 1 else DEFAULT_CSV_PATH
    compare_models(path)