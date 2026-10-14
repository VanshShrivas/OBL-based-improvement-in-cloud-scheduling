#!/usr/bin/env python3
"""
Plots 3-Scenario Convergence Trajectories: Baseline HGA-SSA vs Proposed OBL+HGA-SSA.
Reads: results/multi_scenario_trajectories.csv
Generates: results/multi_scenario_convergence.png
"""

import os
import sys
import pandas as pd
import matplotlib.pyplot as plt

CSV_PATH = os.path.join(os.path.dirname(__file__), "..", "results", "multi_scenario_trajectories.csv")
OUTPUT_PNG = os.path.join(os.path.dirname(__file__), "..", "results", "multi_scenario_convergence.png")

def plot_trajectories():
    if not os.path.exists(CSV_PATH):
        print(f"Error: {CSV_PATH} not found", file=sys.stderr)
        return

    df = pd.read_csv(CSV_PATH)
    scenarios = df["scenario"].unique()

    fig, axes = plt.subplots(1, 3, figsize=(16, 5), dpi=300, sharex=True)

    colors = {
        "base": "#e74c3c",   # Crimson red
        "obl":  "#27ae60"    # Emerald green
    }

    for i, scenario in enumerate(scenarios):
        ax = axes[i]
        sub_df = df[df["scenario"] == scenario]

        ax.plot(sub_df["generation"], sub_df["base_makespan"],
                label="Baseline HGA-SSA (Random Init)", color=colors["base"],
                linewidth=2.2, linestyle="--")

        ax.plot(sub_df["generation"], sub_df["obl_makespan"],
                label="Proposed OBL + HGA-SSA", color=colors["obl"],
                linewidth=2.4)

        ax.set_title(f"{scenario}", fontsize=11, fontweight="bold", pad=10)
        ax.set_xlabel("Generation Number", fontsize=10, fontweight="bold")
        if i == 0:
            ax.set_ylabel("Makespan (seconds)", fontsize=10, fontweight="bold")

        ax.grid(True, linestyle=":", alpha=0.6)
        ax.legend(fontsize=9, loc="upper right", framealpha=0.9)

        # Highlight Gen 0 jumpstart
        gen0_base = sub_df.iloc[0]["base_makespan"]
        gen0_obl  = sub_df.iloc[0]["obl_makespan"]
        ax.scatter([0], [gen0_base], color=colors["base"], s=45, zorder=5)
        ax.scatter([0], [gen0_obl],  color=colors["obl"],  s=45, zorder=5)

    plt.suptitle("Convergence Speed Comparison: Baseline HGA-SSA vs OBL-Enhanced HGA-SSA",
                 fontsize=14, fontweight="bold", y=1.02)
    plt.tight_layout()
    plt.savefig(OUTPUT_PNG, dpi=300, bbox_inches="tight")
    print(f"Convergence trajectories plot saved to: {OUTPUT_PNG}")

if __name__ == "__main__":
    plot_trajectories()
