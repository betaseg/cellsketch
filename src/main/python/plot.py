import os

import matplotlib.pyplot as plt
import numpy as np
import pandas
import seaborn as sns
from matplotlib.ticker import PercentFormatter

color_random = '#000000'

random_dist = {
    'microtubules_individual.csv': {
        'distance of MT end 1 to membrane in um': [
            "membrane_distance_map",
            "membrane_full",
            None
        ],
        'distance of MT end 1 to centrioles in um': [
            "centrioles_distance_map",
            "membrane_full",
            "centrioles"
        ],
        'distance of MT end 1 to golgi in um': [
            "golgi_distance_map",
            "membrane_full",
            'golgi'
        ],
        'distance of MT end 1 to nucleus in um': [
            "nucleus_distance_map",
            "membrane_full",
            'nucleus'
        ]
    },
    'granules_individual.csv': {
        'distance to microtubule in um': [
            "microtubules_distance_map",
            "membrane_full",
            "microtubules"
        ],
        'distance to membrane in um': [
            "membrane_distance_map",
            "membrane_full",
            None
        ],
        'distance to nucleus in um': [
            "nucleus_distance_map",
            "membrane_full",
            "nucleus"
        ],
        'distance to golgi in um': [
            "golgi_distance_map",
            "membrane_full",
            "golgi"
        ]
    }
}


def plot_kde(table, column, min_x, max_x, label, color, style, bw):
    if column not in table:
        return
    data = table[column]
    # sns.kdeplot()
    sns.distplot(data, hist=False, color=color, kde_kws=dict(linestyle=style, bw=bw, label=label, clip=(min_x, max_x)))


def read_table(cell, table_name):
    return pandas.read_table(os.path.join(cell.path, 'misc', cell.name + "_" + table_name), delimiter=',')


def subplot_histogram(cell, table_name, column, min_x, max_x, max_y, ax, pixel_to_um):
    table = read_table(cell, table_name)
    if column not in table:
        return
    data = table[column]
    kws = dict(range=(min_x, max_x + (max_x - min_x)), bins=60)
    ax.hist(data, histtype="stepfilled", color=cell.color, weights=np.ones(len(data)) / len(data), **kws)
    ax2 = ax.twinx()  # instantiate a second axes that shares the same x-axis
    ax2.hist(data, histtype="step", cumulative=True, linestyle="dotted", color="#FF0000",
             weights=np.ones(len(data)) / len(data), **kws)
    if random_dist[table_name] is not None:
        data_random = get_random_distribution(cell, table_name, column, pixel_to_um)
        if data_random is not None:
            ax.hist(data_random, histtype="step", label="random distribution", color=color_random,
                    weights=np.ones(len(data_random)) / len(data_random), **kws)
            ax2.hist(data_random, histtype="step", cumulative=True, linestyle="dotted", color="#212122",
                     weights=np.ones(len(data_random)) / len(data_random), **kws)
    ax.yaxis.set_major_formatter(PercentFormatter(1))
    ax2.yaxis.set_major_formatter(PercentFormatter(1))
    ax.set_xlim([min_x, max_x])
    ax.set_ylim([0, max_y])
    ax2.set_ylim([0, 1])
    ax2.tick_params(axis='y', labelcolor="#FF0000")


def get_random_distribution(cell, table, column, pixel_to_um):
    table_ = random_dist[table]
    if table_ is None:
        return None
    if column not in table_:
        return None
    columns_ = table_[column]
    distance_map = columns_[0]
    mask_in = random_dist[table][column][1]
    mask_out = random_dist[table][column][2]
    mask_nucleus = "nucleus"
    data = get_histogram_data(cell.read_volume(distance_map), cell.read_volume(mask_in),
                              cell.read_volume(mask_out), cell.read_volume(mask_nucleus), pixel_to_um)
    if table == 'granules_individual.csv':
        mean_volume = read_table(cell, 'granules.csv')['mean size in micrometer^3'][0]
        mean_radius = (3. / (4. * np.pi) * mean_volume) ** (1. / 3.)
        # data = np.clip(data - mean_radius, 0, None)
        data = data - mean_radius
        data = data[data >= 0]
    return data


def get_histogram_data(distance_map, mask_in, mask_out, mask_nucleus, pixel_to_um):
    mask = mask_in > 0
    if mask_out is not None:
        mask = mask & (mask_out == 0)
    mask = mask & (mask_nucleus == 0)
    distance_map = np.array(distance_map)
    distances = distance_map[mask] * pixel_to_um
    return distances


def subplot_histogram_compare(table, column, min_x, max_x, ax, compare_column, text_true, text_false, color_true, color_false):
    table_true = table[(table[compare_column] == True)]
    table_false = table[(table[compare_column] == False)]
    data_true = table_true[column]
    data_false = table_false[column]
    data = [data_true, data_false]
    kws = dict(histtype="stepfilled", alpha=0.8, linewidth=2, bins=80, range=(0, max_x * 2))
    ax.hist(data, label=[text_true, text_false], color=[color_true, color_false],
            weights=[np.ones(len(data_true)) / len(data_true), np.ones(len(data_false)) / len(data_false)], **kws)
    ax.yaxis.set_major_formatter(PercentFormatter(1))
    ax.set_xlim([min_x, max_x])
    ax.legend()


def subplot_violin_compare(cell, table, column, ax, compare_column, text_true, text_false, color_true, color_false):
    data_table = read_table(cell, table)
    data_table['dummy'] = 0
    sns.violinplot(data=data_table, x='dummy', y=column, hue=compare_column, split=True,
                   palette=(color_false, color_true), ax=ax)
    ax.legend(handles=ax.legend_.legendHandles, labels=[text_false, text_true])
    ax.spines['top'].set_visible(False)
    ax.spines['right'].set_visible(False)
    ax.spines['bottom'].set_visible(False)
    ax.set_xlabel("")
    ax.get_xaxis().set_ticks([])


def subplot_violin(cell, table, column, ax):
    data_table = read_table(cell, table)
    data_table['dummy'] = 0

    sns.violinplot(y="dummy", x=column, data=data_table, orient='h', inner=None, color=cell.color, ax=ax)
    xlim = ax.get_xlim()
    ylim = ax.get_ylim()
    for violin in ax.collections:
        bbox = violin.get_paths()[0].get_extents()
        x0, y0, width, height = bbox.bounds
        violin.set_clip_path(plt.Rectangle((x0, y0), width, height / 2, transform=ax.transData))

    old_len_collections = len(ax.collections)
    sns.stripplot(y="dummy", x=column, data=data_table, ax=ax, orient='h', color=cell.color, size=3)
    for dots in ax.collections[old_len_collections:]:
        dots.set_offsets(dots.get_offsets() + np.array([0, 0.12]))
    ax.set_xlim(xlim)
    ax.set_ylim(ylim)
    ax.spines['top'].set_visible(False)
    ax.spines['right'].set_visible(False)
    ax.spines['left'].set_visible(False)
    ax.set_ylabel("")
    ax.get_yaxis().set_ticks([])


def lighten_color(color, amount=0.5):
    """
    Lightens the given color by multiplying (1-luminosity) by the given amount.
    Input can be matplotlib color string, hex string, or RGB tuple.

    Examples:
    >> lighten_color('g', 0.3)
    >> lighten_color('#F034A3', 0.6)
    >> lighten_color((.3,.55,.1), 0.5)
    """
    import matplotlib.colors as mc
    import colorsys
    try:
        c = mc.cnames[color]
    except:
        c = color
    c = colorsys.rgb_to_hls(*mc.to_rgb(c))
    return colorsys.hls_to_rgb(c[0], 1 - amount * (1 - c[1]), c[2])
