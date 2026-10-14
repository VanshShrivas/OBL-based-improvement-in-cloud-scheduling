#!/usr/bin/env python3
"""
Plots the convergence curve of the OBL-Enhanced HGA-SSA Scheduler.
Reads: results/convergence.csv
Generates: results/convergence_curve.png
"""

import os
import sys
import pandas as pd
import matplotlib.pyplot as plt

# Paths
DEFAULT_CSV_PATH = os.path.join(os.path.dirname(__file__), "..", "results", "convergence.csv")
OUTPUT_PLOT_PATH = os.path.join(os.path.dirname(__file__), "..", "results", "convergence_curve.png")
BASELINE_MAKESPAN = 115.60  # ICTCS 2025 Paper Baseline


def plot_convergence(csv_path=DEFAULT_CSV_PATH, output_img=OUTPUT_PLOT_PATH):
    # 1. Validate file existence
    if not os.path.exists(csv_path):
        print(f"Error: Convergence file not found at: {csv_path}", file=sys.stderr)
        print("Please run the Java scheduler simulation first to generate results/convergence.csv", file=sys.stderr)
        sys.exit(1)

    # 2. Load dataset
    df = pd.read_csv(csv_path)
    required_cols = {"generation", "fitness", "makespan"}
    if not required_cols.issubset(df.columns):
        print(f"Error: CSV is missing required columns: {required_cols - set(df.columns)}", file=sys.stderr)
        sys.exit(1)

    print(f"Loaded convergence data: {len(df)} generations recorded.")

    # 3. Create High-Quality Dual-Axis Figure
    fig, ax1 = plt.subplots(figsize=(10, 6), dpi=300)

    # Color scheme
    color_fitness = "#1f77b4"   # Academic navy blue
    color_makespan = "#d62728"  # Crimson red
    color_baseline = "#2ca02c"  # Emerald green

    # Left Y-Axis: Multi-Objective Fitness F(i)
    ax1.set_xlabel("Generation Number", fontsize=12, fontweight="bold")
    ax1.set_ylabel("Multi-Objective Fitness F(i) (Lower is better)", color=color_fitness, fontsize=12, fontweight="bold")
    line1 = ax1.plot(
        df["generation"],
        df["fitness"],
        color=color_fitness,
        linewidth=2.2,
        marker="o",
        markersize=4,
        label="Fitness F(i)"
    )
    ax1.tick_params(axis="y", labelcolor=color_fitness)
    ax1.grid(True, linestyle="--", alpha=0.5)

    # Right Y-Axis: Makespan in Seconds
    ax2 = ax1.twinx()
    ax2.set_ylabel("Makespan (seconds)", color=color_makespan, fontsize=12, fontweight="bold")
    line2 = ax2.plot(
        df["generation"],
        df["makespan"],
        color=color_makespan,
        linewidth=2.0,
        linestyle="-.",
        marker="s",
        markersize=4,
        label="Makespan (s)"
    )
    ax2.tick_params(axis="y", labelcolor=color_makespan)

    # Add Horizontal Reference Line for the Paper's 115.60s Baseline
    line3 = ax2.axhline(
        y=BASELINE_MAKESPAN,
        color=color_baseline,
        linestyle="--",
        linewidth=1.8,
        label=f"Paper Baseline ({BASELINE_MAKESPAN}s)"
    )

    # Title & Legend
    plt.title("Convergence Trajectory of OBL-Enhanced HGA-SSA Scheduler", fontsize=14, fontweight="bold", pad=15)
    
    # Combined legend for both axes
    lines = line1 + line2 + [line3]
    labels = [l.get_label() for l in lines]
    ax1.legend(lines, labels, loc="upper right", framealpha=0.9, fontsize=10)

    # Adjust layout and save
    plt.tight_layout()
    os.makedirs(os.path.dirname(output_img), exist_ok=True)
    plt.savefig(output_img, dpi=300)
    print(f"Convergence plot saved successfully to: {output_img}")


if __name__ == "__main__":
    csv_file = sys.argv[1] if len(sys.argv) > 1 else DEFAULT_CSV_PATH
    plot_convergence(csv_file)