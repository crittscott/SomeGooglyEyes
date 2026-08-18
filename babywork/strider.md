Covered by HierarchicalResolver (StriderModel extends HierarchicalModel, no renderToBuffer override —
just the plain `root()` render). Has a baby form; since the model itself does no special baby wrap (only
the general outer age scale every mob gets, which is applied before our render layer runs and so is
inherited automatically), there's nothing for the resolver to miss.
