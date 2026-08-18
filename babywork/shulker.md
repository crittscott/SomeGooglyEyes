Not matched by HierarchicalResolver or AgeableListResolver — ShulkerModel extends ListModel extends
EntityModel directly, so it falls to ChildMapResolver's positional fallback. Functionally fine anyway:
Shulker has no baby form, and ListModel's default `renderToBuffer` just renders `parts()` directly with no
scale/translate wrap, so there's nothing for the fallback to miss.
