# Extract-inner-class recipe — backlog

## Why

Classes over the size threshold (`Ch10_1`) need to be split. Today the plugin flags the size violation but offers no fix recipe — the user has to pick what to extract by hand. Often the natural split is "this batch of methods + the field they share live as a private inner class". A detection-only recipe can surface the candidate; a paired transform can do the easy half automatically.

## Scope

### Detection — `recipes/InnerClassCandidateRecipe`

Within a single `J.ClassDeclaration`, look for a cluster of:

- 1+ private instance field, AND
- 2+ private (or package-private) instance methods that read/write that field, AND
- those methods do not reference any *other* field of the enclosing class.

When such a cluster exists, emit a finding tagged `Ch10_1` (or a new `Ch10_3`) with metadata `{ "fields": "...", "methods": "..." }` and confidence MEDIUM.

**Skip** when:
- The cluster's methods share the enclosing class's `this` (e.g. call `enclosingMethod()`).
- The cluster size is the entire class — split offers nothing.
- The class is already small enough (< threshold).
- The cluster's methods are `@Override` (the inner class can't subclass without re-routing).

### Refactoring — `refactoring/ExtractInnerClassRecipe`

For a detected cluster, transform:

1. Create a `private static final class <ClusterName>` (or non-static if cluster reads the enclosing field non-statically — fall through to non-static if any cluster method touches non-static state outside the cluster).
2. Move the cluster's fields and methods into the inner class.
3. Replace each cluster-method call site `clusterMethod(...)` with `cluster.clusterMethod(...)` where `cluster` is a new private final field of type `<ClusterName>`.
4. Initialise the cluster field in the existing constructor(s); if no constructor, add a default one.
5. Run `RenameRecipe` after to drop redundant `private` modifiers (inner class members are accessible to the enclosing).

**Hard cases (skip with a soft warning to the report):**
- Cluster references the enclosing class's `this` outside the cluster fields.
- Cluster method visibility wider than `private` (callers outside the file would break).
- Cluster has a member named the same as a field elsewhere in the enclosing class (rename collision).

## Naming

Cluster name comes from the *dominant noun* in the cluster's method names:
- `decodeFooBytes` + `validateFooBytes` + `serialiseFooBytes` → `FooBytes`.
- Conservative fallback: `<EnclosingName>InnerXxx` where Xxx is the first letter of the field name.

## Acceptance

- `InnerClassCandidateRecipeTest` — fixtures for: clean cluster (flag), one-method cluster (skip), cluster touching outside fields (skip), entire-class cluster (skip).
- `ExtractInnerClassRecipeTest` — golden-output tests for: simple 1-field/2-method extraction, static-eligible cluster, non-static cluster needing constructor wiring.
- Round-trip property: applying the recipe to its own output should be a no-op.
- After landing the detection recipe, sweep the harness and confirm zero false positives.

## Backlog status

- [ ] `InnerClassCandidateRecipe` (detection)
- [ ] `OpenRewriteFindingSource` mapping
- [ ] `ExtractInnerClassRecipe` (transform)
- [ ] Wired into `RefactoringRegistry` (Ch10_1 entry gets the new recipe alongside the existing manual-only finding)
- [ ] Sweep + sanity test against plugin's own oversize classes
