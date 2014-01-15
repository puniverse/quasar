require 'nokogiri'

# Tutorials:
# http://ruby.bastardsbook.com/chapters/html-parsing/
# http://nokogiri.org/tutorials

module TocFilter
  def toc(html)
    doc = Nokogiri::HTML(html)
    tocul = Nokogiri::XML::Node.new "ul", doc
    tocul['id'] = 'docs-sidemenu'
    ul = tocul
    li = nil
    prev_level = nil
    doc.xpath('//h1 | //h2 | //h3').each do |h| # xpath("//*[@id='markdown-toc']")
      level = h.name[1].to_i
      if prev_level.nil?
        prev_level = level
      end
      if level > prev_level
        ul = Nokogiri::XML::Node.new "ul", doc
        li.add_child ul

        pl = prev_level + 1
        while pl < level do
          li = Nokogiri::XML::Node.new "li", doc
          ul.add_child li
          ul = Nokogiri::XML::Node.new "ul", doc
          li.add_child ul
          pl = pl + 1
        end
      elsif level < prev_level
        pl = prev_level
        while pl > level do
          ul = (ul == tocul ? ul : ul.parent.parent)
          pl = pl - 1
        end
      end
      prev_level = level

      # puts "#{h.name}: #{h.text}" + '#' + h['id'].to_s
      li = Nokogiri::XML::Node.new "li", doc
      ul.add_child li

      a = Nokogiri::XML::Node.new "a", doc
      li.add_child a
      a['href'] = ('#' + h['id'])
      a.content = h.text
      if level == 1
        li['class'] = 'li1'
      elsif level == 2
        a['class'] = 'cat-link'
      end
    end
    tocul.to_html
  end
end

Liquid::Template.register_filter(TocFilter)

# module TocFilter
#   def toc(html)
#     output = ""
#     ul = Nokogiri::HTML(html).css('#markdown-toc')
#     ul.each do |elem| # xpath("//*[@id='markdown-toc']")
#       # elem is main toc ul
#       elem['id'] = 'docs-sidemenu'
#       elem.children.each do |li1|
#         # li1 is li level 1
#         li1['class'] = 'li1'
#         li1.children.select{ |x| x.name == 'ul'}.each do |c|
#           # c is ul under li1
#           c.children.each do |li2|
#             # li2 is li under c
#             li2.children.select{ |x| x.name == 'a'}.each do |a|
#               # a is "a href" under li level 2
#               a['class'] = 'cat-link'
#             end
#           end
#         end
#       end
#     end
#     ul.to_html
#   end
# end
