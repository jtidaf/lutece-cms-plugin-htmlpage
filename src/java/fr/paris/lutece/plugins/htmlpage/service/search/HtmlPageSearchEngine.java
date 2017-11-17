package fr.paris.lutece.plugins.htmlpage.service.search;

import fr.paris.lutece.portal.service.search.IndexationService;
import fr.paris.lutece.portal.service.search.LuceneSearchEngine;
import fr.paris.lutece.portal.service.search.SearchEngine;
import fr.paris.lutece.portal.service.search.SearchItem;
import fr.paris.lutece.portal.service.search.SearchResult;
import fr.paris.lutece.portal.service.util.AppLogService;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.lucene.document.DateTools;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;


/**
 * 
 * HtmlPageSearchEngine
 * 
 */
public class HtmlPageSearchEngine implements SearchEngine
{
    /**
     * Return search results
     * @param strQuery The search query
     * @param request The HTTP request
     * @return Results as a collection of SearchResult
     */
    public List<SearchResult> getSearchResults( String strQuery, HttpServletRequest request )
    {
        ArrayList<SearchItem> listResults = new ArrayList<SearchItem>( );
        IndexSearcher searcher = null;

        try( Directory directory = IndexationService.getDirectoryIndex( ) ; IndexReader ir = DirectoryReader.open( directory ) ; )
        {
            searcher = new IndexSearcher( ir );

            BooleanQuery.Builder queryBuilder = new BooleanQuery.Builder( );

            // Contents
            if ( ( strQuery != null ) && !strQuery.equals( "" ) )
            {
                QueryParser parser = new QueryParser(
                        SearchItem.FIELD_CONTENTS, IndexationService.getAnalyser( ) );
                queryBuilder.add( parser.parse( strQuery ), BooleanClause.Occur.MUST );
            }

            // Type
            Query queryType = new TermQuery( new Term( SearchItem.FIELD_TYPE, HtmlPageIndexer.PROPERTY_INDEX_TYPE_PAGE ) );
            queryBuilder.add( queryType, BooleanClause.Occur.MUST );

            // Get results documents
            TopDocs topDocs = searcher.search( queryBuilder.build( ), LuceneSearchEngine.MAX_RESPONSES );
            ScoreDoc[] hits = topDocs.scoreDocs;

            for ( int i = 0; i < hits.length; i++ )
            {
                int docId = hits[i].doc;
                Document document = searcher.doc( docId );
                SearchItem si = new SearchItem( document );
                listResults.add( si );
            }
        }
        catch ( Exception e )
        {
            AppLogService.error( e.getMessage( ), e );
        }

        return convertList( listResults );
    }

    /**
     * Convert a list of Lucene items into a list of generic search items
     * @param listSource The list of Lucene items
     * @return A list of generic search items
     */
    private List<SearchResult> convertList( List<SearchItem> listSource )
    {
        List<SearchResult> listDest = new ArrayList<SearchResult>( );

        for ( SearchItem item : listSource )
        {
            SearchResult result = new SearchResult( );
            result.setId( item.getId( ) );

            try
            {
                result.setDate( DateTools.stringToDate( item.getDate( ) ) );
            }
            catch ( ParseException e )
            {
                AppLogService.error( "Bad Date Format for indexed item \"" + item.getTitle( ) + "\" : "
                        + e.getMessage( ) );
            }

            result.setUrl( item.getUrl( ) );
            result.setTitle( item.getTitle( ) );
            result.setType( item.getType( ) );
            listDest.add( result );
        }

        return listDest;
    }
}
