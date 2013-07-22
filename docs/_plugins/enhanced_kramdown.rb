require 'kramdown'
require 'pygments'
require 'typogruby'

module Kramdown
  module Converter
    class Pygs < Html
      def convert_codeblock(el, indent)
        attr = el.attr.dup
        lang = extract_code_language!(attr)
        if lang
          add_code_tags(
            Pygments.highlight(el.value,
             :lexer => lang,
             :options => { :encoding => 'utf-8', :linespans => 'line' }) 
            )
        else
          "<pre><code>#{el.value}</code></pre>"
        end
      end

      def add_code_tags(code)
        code = code.sub(/<pre>/,'<pre><code>')
        code = code.sub(/<\/pre>/,"</code></pre>")
      end
    end
  end
end

class Hash
  def symbolize_keys!
    keys.each do |key|
      self[(key.to_sym rescue key) || key] = delete(key)
    end
    self
  end

  def symbolize_keys
    dup.symbolize_keys!
  end
end

module Jekyll
  module Converters
    class Markdown
      class EnhancedKramdownParser
        def initialize(config)
          @config = config
        end

        def convert(content)
          html = Kramdown::Document.new(content, @config["kramdown"].symbolize_keys).to_pygs
          return Typogruby.improve(html)
        end
      end
    end
  end
end