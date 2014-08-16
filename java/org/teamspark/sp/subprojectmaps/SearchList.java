package org.teamspark.sp.subprojectmaps;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;


import org.teamspark.sp.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;

/**
 * Класс выводит список корпусов при вызове с активности и передает номер выбраного
 *
 */
public class SearchList extends ListFragment {
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.list_search_fragment, null);
    }

    public interface searchHouseListener {
        void findByHousingFromFragment(int numberOfHouse);
        void setNumberOfSearchedHouse(int neededHouse);
    }

    searchHouseListener houseListener;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            houseListener = (searchHouseListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement searchHouseListener");
        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (savedInstanceState == null)
            setRetainInstance(true);

        ArrayAdapter<Integer> adapter = new ArrayAdapter<Integer>(getActivity(),
                android.R.layout.simple_list_item_1, getListOfHouses());
        setListAdapter(adapter);
    }

    public void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
        int searchedHouse = getListOfHouses().get((int)id);
        houseListener.findByHousingFromFragment(searchedHouse);
        houseListener.setNumberOfSearchedHouse(searchedHouse);
    }

    private ArrayList<Integer> getListOfHouses(){
        ArrayList<Integer> list = new ArrayList<Integer>();
        Iterator<Integer> it = MapActivity.getHousing()
                .keySet().iterator();
        while (it.hasNext()) {
            Integer key = it.next();
            list.add(key);
        }
        Collections.sort(list);
        return list;

    }
}
