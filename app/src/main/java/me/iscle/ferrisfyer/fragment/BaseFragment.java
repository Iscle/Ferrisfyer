package me.iscle.ferrisfyer.fragment;

import androidx.fragment.app.Fragment;

import me.iscle.ferrisfyer.Ferrisfyer;

public class BaseFragment extends Fragment {
    public Ferrisfyer getFerrisfyer() {
        return (Ferrisfyer) getActivity().getApplication();
    }
}
