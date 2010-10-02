(ns narrative-chains.coref
  (:import [opennlp.tools.coref DefaultLinker DiscourseEntity Linker LinkerMode]
           [opennlp.tools.coref.mention DefaultParse Mention MentionContext PTBMentionFinder]
           [opennlp.tools.parser Parse]
           [opennlp.tools.parser.chunking Parser]
           [opennlp.tools.util Span]
           [opennlp.tools.lang.english TreebankLinker]))

(defn process-parse [& args] nil)
