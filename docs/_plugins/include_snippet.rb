# Title: Include Snippet Tag for Jekyll
# Author: Eitan Yarden https://github.com/eitan101
# Description: Import your code snippets into any blog post.
# Configuration: You can set default import path in _config.yml (defaults to code_dir: downloads/code)
#
# Syntax {% include_snippet snippet_name path/to/file %}
#
# Example 1:
# ~~~ java
# {% include_code hello world javascripts/test.java %}
# ~~~
#
# This will import test.js starting from the line which contains "snippet hello world" till
# the line which contains "end of snippet". You can use the "snippet_exclude_begin" and "snippet_exclude_end // ..." for
# block exclusion. For code higlight wrap the tag with the appropritate md tag.
#
#

require 'pathname'

module Jekyll

  class IncludeCodeTag < Liquid::Tag
    # include HighlightCode
    # include TemplateWrapper
    def initialize(tag_name, markup, tokens)
      @title = nil
      if markup.strip =~ /(.*)?(\s+|^)(\/*\S+)/i
        @title = $1 || nil
        @file = $3
      else
        fail "no title in '#{markup.strip}'"
      end
      super
    end

    def render(context)
      code_dir = (context.registers[:site].config['code_dir'].sub(/^\//,'') || 'downloads/code')
      code_path = (Pathname.new(context.registers[:site].source) + code_dir).expand_path
      file = code_path + @file

      if File.symlink?(code_path)
        return "Code directory '#{code_path}' cannot be a symlink"
      end

      unless file.file?
        fail "File #{file} could not be found"
      end

      Dir.chdir(code_path) do
        code = file.read
        source = ""
        inblock = false
        exclude = false
        spaces = -1;
        snippet_found = false
        code.lines.each_with_index do |line,index|
          if line =~ /end of snippet/
            inblock = false
          end
          if (line =~ /snippet_exclude_begin/)
            exclude = true
          end
          if inblock && !exclude
              if spaces==-1
                spaces = line.index(/[^ ]/)
              end
              if spaces>-1 && spaces <= line.index(/[^ ]/)
                line = line[spaces..line.length]
              end
              source  += "#{line}"
          end
          if (line =~ /snippet_exclude_end/)
            # add what comes after "snippet_exclude_end" as a block replacement
            # for example "// ..."
            # update: disabled. causes problems in cases like <!--snippet_exclude_end-->
            # index = line.index(/snippet_exclude_end/)
            # if (index) + 20 +1 < line.length
            #   source += line[index+20..line.length]
            # end
            exclude = false
          end
          if line =~ /snippet #{@title}/
            inblock = true
            snippet_found = true
          end
        end
        if not snippet_found
          fail "snippet #{@title} not found in #{file}"
        end
        ouput = source
      end
    end
  end

end

Liquid::Template.register_tag('include_snippet', Jekyll::IncludeCodeTag)
