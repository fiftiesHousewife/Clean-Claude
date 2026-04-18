plugins {
    id("cleancode.java-library")
}

// This module is a fixture library for the ReworkOrchestrator's paired runs.
// The classes here deliberately carry G30 / Ch10.1 findings so we have a stable
// benchmark to point the agent at. Rework comparisons mutate these files in
// place and rely on `git restore` between runs — nothing here is production.
