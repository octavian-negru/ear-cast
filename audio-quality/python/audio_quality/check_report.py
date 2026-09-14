"""Apply explicit, predeclared per-condition gates to a report. Missing scores fail."""
import argparse
import json
import math
from pathlib import Path


def failures(report, criteria):
    indexed = {(row["case"], row["variant"]): row for row in report["results"]}
    failed = []
    if len(indexed) != len(report["results"]):
        failed.append("Duplicate case/variant results")
    for case in criteria["cases"]:
        for variant, bounds in criteria["variants"].items():
            if not bounds:
                failed.append(f"{case}/{variant}: empty metric criteria")
            row = indexed.get((case, variant))
            if row is None:
                failed.append(f"{case}/{variant}: missing result")
                continue
            scores = dict(row["metrics"])
            if scores.get("output_silent"):
                failed.append(f"{case}/{variant}: silent output")
            if scores.get("estoi_input") is not None and scores.get("estoi_output") is not None:
                scores["estoi_delta"] = scores["estoi_output"] - scores["estoi_input"]
            for metric, limits in bounds.items():
                if (not limits or set(limits) - {"min", "max"} or
                        any(not isinstance(v, (int, float)) or not math.isfinite(v) for v in limits.values()) or
                        limits.get("min", -math.inf) > limits.get("max", math.inf)):
                    failed.append(f"{case}/{variant}/{metric}: invalid bounds")
                    continue
                value = scores.get(metric)
                if not isinstance(value, (int, float)) or not math.isfinite(value):
                    failed.append(f"{case}/{variant}/{metric}: missing or undefined")
                elif value < limits.get("min", -math.inf) or value > limits.get("max", math.inf):
                    failed.append(f"{case}/{variant}/{metric}: {value} outside {limits}")
    if not criteria["cases"] or not criteria["variants"]:
        failed.append("Empty acceptance criteria")
    return failed


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("report", type=Path)
    parser.add_argument("criteria", type=Path)
    args = parser.parse_args()
    failed = failures(json.loads(args.report.read_text()), json.loads(args.criteria.read_text()))
    print(json.dumps({"passed": not failed, "failures": failed}, indent=2))
    raise SystemExit(1 if failed else 0)


if __name__ == "__main__":
    main()
