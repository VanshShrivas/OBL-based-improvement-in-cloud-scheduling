#!/usr/bin/env python3
"""
Generates formatted schedule distribution tables matching Tables II–IV
from the research paper: "Hybrid Metaheuristic-Based Cloud Task Scheduling
Using Genetic Algorithm and Salp Swarm Algorithm (HGA-SSA)".

Reads: results/best_schedule.csv
Displays:
  - Table: VM ID | Processing Power | Assigned Tasks / Durations | Total Execution Time
"""

import os
import sys
import pandas as pd

DEFAULT_CSV_PATH = os.path.join(os.path.dirname(__file__), "..", "results", "best_schedule.csv")


def generate_schedule_table(csv_path=DEFAULT_CSV_PATH):
    if not os.path.exists(csv_path):
        print(f"Error: Schedule file not found at: {csv_path}", file=sys.stderr)
        print("Please run ResultExporter or the simulation first to produce results/best_schedule.csv", file=sys.stderr)
        sys.exit(1)

    df = pd.read_csv(csv_path)

    # Group task allocations by VM ID
    vm_groups = {}
    for _, row in df.iterrows():
        vm_id = int(row["vm_id"])
        task_id = int(row["task_id"])
        duration = int(row["duration"])
        power = float(row["vm_power"])
        total_time = float(row["vm_total_time"])

        if vm_id not in vm_groups:
            vm_groups[vm_id] = {
                "power": power,
                "tasks": [],
                "total_time": total_time
            }
        vm_groups[vm_id]["tasks"].append(f"Task {task_id} ({duration}s)")

    # Format into Table structure
    table_rows = []
    all_vm_ids = sorted(vm_groups.keys())
    for vm_id in all_vm_ids:
        info = vm_groups[vm_id]
        assigned_tasks_str = ", ".join(info["tasks"])
        table_rows.append({
            "VM ID": vm_id,
            "Processing Power": f"{info['power']:.2f}",
            "Assigned Tasks (Duration)": assigned_tasks_str,
            "Total Execution Time (s)": f"{info['total_time']:.2f}"
        })

    table_df = pd.DataFrame(table_rows)

    print("\n" + "=" * 88)
    print("  TABLE: TASK ALLOCATION AND VM EXECUTION TIMES (OBL-ENHANCED HGA-SSA)")
    print("  (Matches format of Tables II–IV from ICTCS 2025 Paper)")
    print("=" * 88)
    print(table_df.to_string(index=False))
    print("=" * 88)

    # Compute and display overall makespan
    makespan = max(vm_groups[v]["total_time"] for v in all_vm_ids)
    print(f"\nFinal Makespan (Max VM Execution Time): {makespan:.2f} seconds\n")


if __name__ == "__main__":
    path = sys.argv[1] if len(sys.argv) > 1 else DEFAULT_CSV_PATH
    generate_schedule_table(path)