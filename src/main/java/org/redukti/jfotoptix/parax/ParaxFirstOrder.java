package org.redukti.jfotoptix.parax;

import org.redukti.jfotoptix.light.SpectralLine;
import org.redukti.jfotoptix.math.Vector3;
import org.redukti.jfotoptix.model.Element;
import org.redukti.jfotoptix.model.OpticalSurface;
import org.redukti.jfotoptix.model.OpticalSystem;
import org.redukti.jfotoptix.model.Stop;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ParaxFirstOrder {

    double effective_focal_length;
    double back_focal_length;
    double optical_invariant;
    double object_distance;
    double image_distance;
    double power;
    double pp1;
    double ppk;
    double ffl;
    double fno;
    double enp_dist;
    double enp_radius;
    double exp_dist;
    double exp_radius;
    double m;
    double red;
    double n_obj;
    double n_img;
    double img_ht;
    double obj_ang;
    double obj_na;
    double img_na;

    public static ParaxFirstOrder compute(OpticalSystem system) {

        YNUTrace tracer = new YNUTrace();
        List<OpticalSurface> seq = system.get_sequence().stream().filter(e -> e instanceof OpticalSurface).map(e -> (OpticalSurface)e).collect(Collectors.toList());
        // Trace a ray parallel to axis at height 1.0 from infinity
        Map<Integer, Vector3> p_ray = tracer.trace(seq, 1.0, 0.0, -1e10);
        // Trace a ray that has unit angle from infinity
        Map<Integer, Vector3> q_ray = tracer.trace(seq, 1e10, 1.0, -1e10);

        // The following implementation is ported from
        // https://github.com/mjhoptics/ray-optics
        // SD 3-Clause License
        //
        //Copyright (c) 2017-2021, Michael J. Hayford
        //All rights reserved.

        int first = seq.get(0).id();
        int last = seq.get(seq.size()-1).id();
        ParaxFirstOrder pfo = new ParaxFirstOrder();
        double u_k = p_ray.get(last).v(1);
        pfo.effective_focal_length = -p_ray.get(first).v(0)/u_k;
        pfo.back_focal_length = -p_ray.get(last).v(0)/u_k;

        double n = seq.get(0).get_material(0).get_refractive_index(SpectralLine.d);
        double n_k = 1.0*seq.get(seq.size()-1).get_material(1).get_refractive_index(SpectralLine.d); // FIXME 1.0 is z_dir
        double ak1 = p_ray.get(last).v(0);
        double bk1 = q_ray.get(last).v(0);
        double ck1 = n_k * p_ray.get(last).v(1);
        double dk1 = n_k * q_ray.get(last).v(1);

        Stop stop = seq.stream().filter(e-> e instanceof Stop).map(e -> (Stop)e).findFirst().orElse(null);
        double n_s = 1.0*stop.get_material(0).get_refractive_index(SpectralLine.d); // FIXME 1.0 is z_dir
        double as1 = p_ray.get(stop.id()).v(0);
        double bs1 = q_ray.get(stop.id()).v(0);
        double cs1 = n_s*p_ray.get(stop.id()).v(1);
        double ds1 = n_s*q_ray.get(stop.id()).v(1);

        // find entrance pupil location w.r.t. first surface
        double ybar1 = -bs1;
        double ubar1 = as1;
        double n_0 = n;
        double enp_dist = -ybar1/(n_0*ubar1);

        double thi0 = 1e10;
        // calculate reduction ratio for given object distance
        double red = dk1 + thi0*ck1;
        double obj2enp_dist = thi0 + enp_dist;

        String pupil_aperture = "aperture";
        String obj_img_key = "image";
        String value_key = "f/#";
        double pupil_value = 0.98;

        double slp0 = 0.0;
        if (obj_img_key.equals("object")) {
            if (value_key.equals("pupil")) {
                slp0 = 0.5 * pupil_value / obj2enp_dist;
            } else if (value_key.equals("NA")) {
                slp0 = n_0 * Math.tan(Math.asin(pupil_value / n_0));
            }
        }
        else if(obj_img_key.equals("image")) {
            if (value_key.equals("f/#")) {
                double slpk = -1.0 / (2.0 * pupil_value);
                slp0 = slpk / red;
            } else if (value_key.equals("NA")) {
                double slpk = n_k * Math.tan(Math.asin(pupil_value / n_k));
                slp0 = slpk / red;
            }
        }
        double yu_ht = 0.0;
        double yu_slp = slp0;

        String field = "field";
        obj_img_key = "object";
        value_key = "angle";
        double max_fld = 19.98;
        int fn = 1;
        double ang = 0.0;
        double slpbar0 = 0.0;
        double ybar0 = 0.0;

        if (max_fld == 0.0)
            max_fld = 1.0;
        if (obj_img_key.equals("object")) {
            if (value_key.equals("angle")) {
                ang = Math.toRadians(max_fld);
                slpbar0 = Math.tan(ang);
                ybar0 = -slpbar0 * obj2enp_dist;
            } else if (value_key.equals("height")) {
                ybar0 = -max_fld;
                slpbar0 = -ybar0 / obj2enp_dist;
            }
        }
        else if (obj_img_key.equals("image")) {
            if (value_key.equals("height")) {
                ybar0 = red * max_fld;
                slpbar0 = -ybar0 / obj2enp_dist;
            }
        }
        double yu_bar_ht = ybar0;
        double yu_bar_slp = slpbar0;

        // Get height at first surface from object height
        yu_ht = yu_ht + 1e10 * yu_slp;
        yu_bar_ht = yu_bar_ht + 1e10 * yu_bar_slp;

        // We have the starting coordinates, now trace the rays
        Map<Integer, Vector3> ax_ray = tracer.trace(seq, yu_ht, yu_slp, 0.0);
        Map<Integer, Vector3> pr_ray = tracer.trace(seq, yu_bar_ht, yu_bar_slp, 0.0);

        pfo.optical_invariant = n * (ax_ray.get(first).v(0) * yu_bar_slp
                - pr_ray.get(first).v(0) * yu_slp);

        // Fill in the contents of the FirstOrderData struct
        pfo.object_distance = thi0;
        pfo.image_distance = seq.get(seq.size()-1).get_thickness();
        if (ck1 == 0.0) {
            pfo.power = 0.0;
            pfo.effective_focal_length = 0.0;
            pfo.pp1 = 0.0;
            pfo.ppk = 0.0;
        }
        else {
            pfo.power = -ck1;
            pfo.effective_focal_length = -1.0 / ck1;
            pfo.pp1 = (dk1 - 1.0) * (n_0 / ck1);
            pfo.ppk = (p_ray.get(last).v(0) - 1.0) * (n_k / ck1);
            pfo.ffl = pfo.pp1 - pfo.effective_focal_length;
            pfo.back_focal_length = pfo.effective_focal_length - pfo.ppk;
            pfo.fno = -1.0 / (2.0 * n_k * ax_ray.get(last).v(1));
        }
        pfo.m = ak1 + ck1*pfo.image_distance/n_k;
        pfo.red = dk1 + ck1*pfo.object_distance;
        pfo.n_obj = n_0;
        pfo.n_img = n_k;
        pfo.img_ht = -pfo.optical_invariant/(n_k*ax_ray.get(last).v(1));
        pfo.obj_ang = Math.toDegrees(Math.atan(yu_bar_slp));
        if (yu_bar_slp != 0) {
            double nu_pr0 = n_0 * yu_bar_slp;
            pfo.enp_dist = -pr_ray.get(first).v(0) / nu_pr0;
            pfo.enp_radius = Math.abs(pfo.optical_invariant / nu_pr0);
        }
        else {
            pfo.enp_dist = -1e10;
            pfo.enp_radius = 1e10;
        }

        if (pr_ray.get(last).v(1) != 0) {
            pfo.exp_dist = -(pr_ray.get(0).v(0) / pr_ray.get(0).v(1) - pfo.image_distance);
            pfo.exp_radius = Math.abs(pfo.optical_invariant / (n_k * pr_ray.get(0).v(1)));
        }
        else {
            pfo.exp_dist = -1e10;
            pfo.exp_radius = 1e10;
        }

        // compute object and image space numerical apertures
        pfo.obj_na = n_0*Math.sin(Math.atan(1.0*yu_slp)); // FIXME 1.0 is z_dir
        pfo.img_na = n_k*Math.sin(Math.atan(1.0*ax_ray.get(0).v(1))); // FIXME 1.0 is z_dir

        return pfo;
    }

    @Override
    public String toString() {
        return "ParaxFirstOrder{" +
                "effective_focal_length=" + effective_focal_length +
                ", back_focal_length=" + back_focal_length +
                ", optical_invariant=" + optical_invariant +
                ", object_distance=" + object_distance +
                ", image_distance=" + image_distance +
                ", power=" + power +
                ", pp1=" + pp1 +
                ", ppk=" + ppk +
                ", ffl=" + ffl +
                ", fno=" + fno +
                ", enp_dist=" + enp_dist +
                ", enp_radius=" + enp_radius +
                ", exp_dist=" + exp_dist +
                ", exp_radius=" + exp_radius +
                ", m=" + m +
                ", red=" + red +
                ", n_obj=" + n_obj +
                ", n_img=" + n_img +
                ", img_ht=" + img_ht +
                ", obj_ang=" + obj_ang +
                ", obj_na=" + obj_na +
                ", img_na=" + img_na +
                '}';
    }
}
