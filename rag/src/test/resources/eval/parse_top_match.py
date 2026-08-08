"""Reads /api/rag/search JSON on stdin, prints "score<TAB>source<TAB>text" for the top match.

A separate file rather than a python3 -c one-liner inside run-evaluation.sh: the nested
quoting needed to index JSON keys inside a shell heredoc does not survive, and produced a
SyntaxError that read as eight retrieval failures.
"""
import json
import sys

results = json.load(sys.stdin)
if not results or not results[0].get("matches"):
    print("0.0\t(none)\t(no results)")
    sys.exit()

top = results[0]
match = top["matches"][0]
text = match["text"][:72].replace("\t", " ")
print("{:.3f}\t{}\t{}".format(top["topScore"], match["source"], text))
