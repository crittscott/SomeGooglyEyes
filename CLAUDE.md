This is an unreleased Minecraft 1.20.1/Forge mod, not a mission-critical enterprise suite. There is to be no legacy support; no concern for backward compatibility. If we change data formats, we update all the stored data at the time of the change. Unless the data needs an AI to work out the new format, use deterministic tools (CLI utilities, python, etc).

When asked to write a commit message, make it succinct.

When assessing code for safety issues, do not worry that Minecraft or Forge itself may misbehave; it is not our job to protect against every concievable error. Only known unreliable interfaces need to be protected against.

Do not compile, build, or run the project (no `gradlew`, no `runGameTestServer`, etc.) unless I explicitly ask. Your job is to read and write code. Verify your work by reading it and reasoning about it — do not touch the ForgeGradle caches, kill processes, or otherwise rewire the dev environment. I run the builds and tests. If you believe a build or test run is warranted, say so and let me decide.

Code comments are for existing code, not for recording what changed from some historical version, and not for recording future hypotheticals. Just as there is no legacy support in code, there should be no legacy commentary or reference to what was done in the past.