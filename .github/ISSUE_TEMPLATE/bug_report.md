---
name: Bug report
about: Create a report to help us improve

---

**Description:**
A clear and concise description of what the bug is. Additionally, it would help if you could include the logs and the entire stacktrace, including the "caused by" portion. (see GitHub-Markdown docs at: https://guides.github.com/features/mastering-markdown for logs/code formatting guidelines)

**Release versions:**
There is an API (https://docs.spring.io/spring-cloud-dataflow/docs/current/reference/htmlsingle/#api-guide-resources-server-meta-retrieving) to gather SCDF's system information, including the dependent projects and the associated versions. Alternatively, you can capture this information from the Dashboard's About tab (https://docs.spring.io/spring-cloud-dataflow/docs/current/reference/htmlsingle/#dashboard). Please be sure to include the copied JSON in the bug report.

**Custom apps:**
If your Stream or Task data pipeline includes custom apps and there is a problem associated with it, please share the sample-app (add a link to the GitHub repo) and the release versions in use. Also, please be sure to share the register, create, and deploy/launch DSL commands for completeness.

**Steps to reproduce:**
Include the steps to reproduce the behavior. Better yet, if you have a reproducible sample, please attach it in the issue. It can help us to relate to the problem more easily.

**Screenshots:**
Where applicable, add screenshots to help explain your problem.

**Additional context:**
Add any other context about the problem here.
